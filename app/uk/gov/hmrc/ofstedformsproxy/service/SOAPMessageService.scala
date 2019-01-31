/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.ofstedformsproxy.service

import java.io.{File, FileInputStream, FileOutputStream, IOException}
import java.security._
import java.security.cert.{Certificate, CertificateEncodingException}
import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}
import java.util
import java.util.{Base64, UUID}

import com.google.inject.{ImplementedBy, Inject}
import javax.inject.Singleton
import javax.xml.crypto.dom.DOMStructure
import javax.xml.crypto.dsig._
import javax.xml.crypto.dsig.dom.DOMSignContext
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory
import javax.xml.crypto.dsig.spec.{C14NMethodParameterSpec, TransformParameterSpec}
import javax.xml.parsers.ParserConfigurationException
import javax.xml.soap._
import javax.xml.transform.stream.StreamSource
import org.w3c.dom.{DOMException, Document}
import org.xml.sax.SAXException
import play.api.Environment
import scalaz._

import scala.util.{Failure, Success, Try}

@ImplementedBy(classOf[SOAPMessageServiceImpl])
trait SOAPMessageService {

  def readInXMLPayload(path: String): String \/ Document

  def createSOAPEnvelope(document: Document): String \/ SOAPMessage

  def signSOAPMessage(soapMessage: SOAPMessage): String \/ SOAPMessage

  def outputFile(soapMessage : SOAPMessage): String \/ Unit

  def call(soapMessage : SOAPMessage) : String \/ Unit

}

@Singleton
class SOAPMessageServiceImpl @Inject()(env: Environment) extends SOAPMessageService {

  val u0 = UUID.randomUUID.toString
  val u1 = u0 + "-1"
  val u2 = u0 + "-2"

  System.setProperty("javax.xml.soap.MessageFactory", "com.sun.xml.internal.messaging.saaj.soap.ver1_2.SOAPMessageFactory1_2Impl")
  System.setProperty("javax.xml.bind.JAXBContext", "com.sun.xml.internal.bind.v2.ContextFactory")

  def readInXMLPayload(path: String): String \/ org.w3c.dom.Document = {

    env.getExistingFile(path) match {
      case Some(xml) => Try {
        val dbFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance
        dbFactory.setNamespaceAware(true)
        dbFactory.newDocumentBuilder.parse(xml)
      }
      match {
        case Success(document) => \/-(document)
        case Failure(e: ParserConfigurationException) => -\/(e.getMessage)
        case Failure(e: SAXException) => -\/(e.getMessage)
        case Failure(e: IOException) => -\/(e.getMessage)
      }
      case None => {
        -\/("XML payload file does not exist.")
      }
    }
  }

  def createSOAPEnvelope(xmlDocument: Document): String \/ SOAPMessage = Try {
    // Create SOAP Message
    val messageFactory = MessageFactory.newInstance
    val soapMessage = messageFactory.createMessage
    val soapEnvelope = soapMessage.getSOAPPart.getEnvelope
    soapEnvelope.setPrefix("s")
    soapEnvelope.removeNamespaceDeclaration("SOAP-ENV")

    soapEnvelope.addNamespaceDeclaration("a", "http://www.w3.org/2005/08/addressing")
    soapEnvelope.addNamespaceDeclaration("u", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd")
//    soapEnvelope.addNamespaceDeclaration("env", "http://schemas.xmlsoap.org/soap/envelope/")

    // Add DOM object to SOAP body
    val soapBody = soapMessage.getSOAPBody
    soapBody.setPrefix("s")
    soapBody.addDocument(xmlDocument)
    soapBody.addAttribute(soapEnvelope.createName("Id", "u", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"), "_1")
    soapMessage

  } match {
    case Success(soapMessage) => \/-(soapMessage)
    case Failure(e: SOAPException) => -\/(e.getMessage)
    case Failure(e: DOMException) => -\/(e.getMessage)
  }

  override def signSOAPMessage(soapMessage: SOAPMessage): String \/ SOAPMessage = {

    Try {
      val soapHeader: SOAPHeader = soapMessage.getSOAPHeader
      soapHeader.setPrefix("s")

      addAction(soapHeader, soapMessage)

      addMessageId(soapHeader)

      addReplyTo(soapHeader)

      addTo(soapHeader, soapMessage)

      val securityElement = addSecurity(soapHeader, soapMessage)

      //TODO: needs to come from the CustomWSConfigParser
      val cert = getCertificate

      val timestamp = addTimestamp(securityElement, soapMessage)

      addUsernameToken(securityElement, soapMessage)

      addBinarySecurityToken(securityElement, cert)

      addSignature(securityElement, soapMessage.getSOAPBody, timestamp)

      soapMessage
    } match {
      case Success(signedSoapMessage) => \/-(signedSoapMessage)
      case Failure(e: SOAPException) => -\/(e.getMessage)
      case Failure(e: DOMException) => -\/(e.getMessage)
      case Failure(e: NoSuchAlgorithmException) => -\/(e.getMessage)
      case Failure(e: KeyStoreException) => -\/(e.getMessage)
      case Failure(e: CertificateEncodingException) => -\/(e.getMessage)
      case Failure(e: InvalidAlgorithmParameterException) => -\/(e.getMessage)
      case Failure(e: XMLSignatureException) => -\/(e.getMessage)
    }

  }

  @throws[Exception]
  private def getKeyFormCert = {
    val password = "tdLK!PEV8}Gb5CM"
    // Get cert password.
    // (i) Get byte array of password
    val passwordByte = password.getBytes
    // (ii) Get MD5 Hash of byte array
    val digest = java.security.MessageDigest.getInstance("MD5")
    val passwordHashed = digest.digest(passwordByte)
    // (iii) Base64 encode hashed byte array
    val passwordHashedBase64 = Base64.getEncoder.encodeToString(passwordHashed)
    // (iv) Open the cert using KeyStore
    val keystore = KeyStore.getInstance("jks")
    keystore.load(new FileInputStream(new File("/home/mikail/Tmp/Ofsted/ofsted.jks")), password.toCharArray)
    // (v) Extract Private Key
    val key = keystore.getKey("le-externalextranetuser!00282yearsha22003template!0029-7c3ba8e1-ea15-421f-bd7a-a3cfea301c83", password.toCharArray).asInstanceOf[PrivateKey]
    key
  }

  @throws[SOAPException]
  private def addSecurityToken(signature: SOAPElement) = {
    val securityTokenReference = signature.addChildElement("SecurityTokenReference", "o")
    val reference = securityTokenReference.addChildElement("Reference", "o")
    reference.setAttribute("URI", String.format("#uuid-%s", u2))
    reference.setAttribute("ValueType", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3")
    securityTokenReference
  }

  //TODO: wrap in Try
  private def addSignature(securityElement: SOAPElement, soapBody: SOAPBody, timestamp: SOAPElement) = { // Get private key from ROS digital certificate
    val key = getKeyFormCert
    val securityTokenReference = addSecurityToken(securityElement)
    // Add signature
    createDetachedSignature(securityElement, key, securityTokenReference, soapBody, timestamp)
    securityElement
  }

  @throws[Exception]
  private def createDetachedSignature(signatureElement: SOAPElement, privateKey: PrivateKey, securityTokenReference: SOAPElement, soapBody: SOAPBody, timestamp: SOAPElement): Unit = {
    val providerName = System.getProperty("jsr105Provider", "org.jcp.xml.dsig.internal.dom.XMLDSigRI")
    val xmlSignatureFactory = XMLSignatureFactory.getInstance("DOM", Class.forName(providerName).newInstance.asInstanceOf[Provider])
    //Digest method
    val digestMethod = xmlSignatureFactory.newDigestMethod(DigestMethod.SHA1, null)
    val transformList = new util.ArrayList[Transform]
    //Transform
    val envTransform = xmlSignatureFactory.newTransform("http://www.w3.org/2001/10/xml-exc-c14n#", null.asInstanceOf[TransformParameterSpec])
    transformList.add(envTransform)
    //References
    val refList = new util.ArrayList[Reference]
    val refTS = xmlSignatureFactory.newReference("#_0", digestMethod, transformList, null, null)
    val refBody = xmlSignatureFactory.newReference("#_1", digestMethod, transformList, null, null)
    refList.add(refTS)
    refList.add(refBody)
    val cm = xmlSignatureFactory.newCanonicalizationMethod("http://www.w3.org/2001/10/xml-exc-c14n#", null.asInstanceOf[C14NMethodParameterSpec])
    val sm = xmlSignatureFactory.newSignatureMethod("http://www.w3.org/2000/09/xmldsig#rsa-sha1", null)
    val signedInfo = xmlSignatureFactory.newSignedInfo(cm, sm, refList)
    val signContext = new DOMSignContext(privateKey, signatureElement)
    // signContext.setDefaultNamespacePrefix("ds");
    //signContext.putNamespacePrefix("http://www.w3.org/2000/09/xmldsig#", "ds");
    //These are required for new Java versions
    signContext.setIdAttributeNS(soapBody, "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd", "Id")
    signContext.setIdAttributeNS(timestamp, "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd", "Id")
    val keyFactory = KeyInfoFactory.getInstance
    val domKeyInfo = new DOMStructure(securityTokenReference)
    val keyInfo = keyFactory.newKeyInfo(java.util.Collections.singletonList(domKeyInfo))
    val signature = xmlSignatureFactory.newXMLSignature(signedInfo, keyInfo)
    signContext.setBaseURI("")
    signature.sign(signContext)
  }

  //TODO: wrap in Try
  private def addBinarySecurityToken(securityElement: SOAPElement, cert: Certificate) = { // Get byte array of cert.
    val certByte = cert.getEncoded
    // Add the Binary Security Token element
    val binarySecurityToken = securityElement.addChildElement("BinarySecurityToken", "o")
    binarySecurityToken.setAttribute("u:Id", String.format("uuid-%s", u2))
    binarySecurityToken.setAttribute("ValueType", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3")
    binarySecurityToken.setAttribute("EncodingType", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary")
    binarySecurityToken.addTextNode(Base64.getEncoder.encodeToString(certByte))
    securityElement
  }

  //TODO: wrap in Try
  private def addTimestamp(securityElement: SOAPElement, soapMessage: SOAPMessage) = {
    val timestamp = securityElement.addChildElement("Timestamp", "u")
    val soapEnvelope = soapMessage.getSOAPPart.getEnvelope
    timestamp.addAttribute(soapEnvelope.createName("Id", "u", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"), "_0")
    val DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSX"
    val timeStampFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)
    timestamp.addChildElement("Created", "u").setValue(timeStampFormatter.format(ZonedDateTime.now.toInstant.atZone(ZoneId.of("UTC"))))
    timestamp.addChildElement("Expires", "u").setValue(timeStampFormatter.format(ZonedDateTime.now.plusSeconds(300).toInstant.atZone(ZoneId.of("UTC"))))
    //        timestamp.addChildElement("Created", "u").setValue("2019-01-07T11:08:30.000Z");
    //        timestamp.addChildElement("Expires", "u").setValue("2019-01-07T11:12:35.000Z");
    timestamp
  }

  //TODO: wrap in Try
  private def addAction(soapHeader: SOAPElement, soapMessage: SOAPMessage) = {
    val action = soapHeader.addChildElement("Action", "a")
    val soapEnvelope = soapMessage.getSOAPPart.getEnvelope
    action.addAttribute(soapEnvelope.createName("mustUnderstand", "s", "http://www.w3.org/2003/05/soap-envelope"), "1")
    action.addTextNode("http://tempuri.org/IGatewayOOServices/GetData") //TODO: needs dynamic

    action
  }

  //TODO: wrap in Try
  private def addMessageId(soapHeader: SOAPElement) = {
    val messageId = soapHeader.addChildElement("MessageID", "a")
    messageId.addTextNode(String.format("urn:uuid:%s", UUID.randomUUID.toString))
    messageId
  }

  //TODO: wrap in Try
  private def addReplyTo(soapHeader: SOAPElement) = {
    val replyTo = soapHeader.addChildElement("ReplyTo", "a")
    val address = replyTo.addChildElement("Address", "a")
    address.addTextNode("http://www.w3.org/2005/08/addressing/anonymous")
    replyTo
  }

  //TODO: wrap in Try
  private def addTo(soapHeader: SOAPElement, soapMessage: SOAPMessage) = {
    val to = soapHeader.addChildElement("To", "a")
    val soapEnvelope = soapMessage.getSOAPPart.getEnvelope
    to.addAttribute(soapEnvelope.createName("mustUnderstand", "s", "http://www.w3.org/2003/05/soap-envelope"), "1")
    to.addAttribute(soapEnvelope.createName("Id", "u", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"), "_1")
    to.addTextNode("https://testinfogateway.ofsted.gov.uk/OnlineOfsted/GatewayOOServices.svc")
    to
  }

  //TODO: wrap in Try
  private def addUsernameToken(securityElement: SOAPElement, soapMessage: SOAPMessage) = {
    val usernameToken = securityElement.addChildElement("UsernameToken", "o")
    val soapEnvelope = soapMessage.getSOAPPart.getEnvelope
    usernameToken.addAttribute(soapEnvelope.createName("Id", "u", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"), String.format("uuid-%s", u1))
    usernameToken.addChildElement("Username", "o").setValue("extranet\\hmrcgforms")
    val e = usernameToken.addChildElement("Password", "o")
    e.addAttribute(soapEnvelope.createName("Type", "o", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"), "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText")
    e.addTextNode("MX(6ZvLS7wmt~2\\")
    usernameToken
  }


  //TODO: wrap in Try
  private def addSecurity(soapHeader: SOAPElement, soapMessage: SOAPMessage) = {
    val security = soapHeader.addChildElement("Security", "o", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd")
    val soapEnvelope = soapMessage.getSOAPPart.getEnvelope
    security.addAttribute(soapEnvelope.createName("mustUnderstand", "s", "http://www.w3.org/2003/05/soap-envelope"), "1")
    security
  }

  //TODO: wrap in Try
  //TODO: this must reference the JKS that's in the CustomWSConfigParser
  private def getCertificate = {
    val password = "tdLK!PEV8}Gb5CM"
    // (i) Get byte array of password
    val passwordByte = password.getBytes
    // (ii) Get MD5 Hash of byte array
    val digest = java.security.MessageDigest.getInstance("MD5")
    val passwordHashed = digest.digest(passwordByte)
    // (iii) Base64 encode hashed byte array
    val passwordHashedbase64 = Base64.getEncoder.encodeToString(passwordHashed)
    // (iv) Open the Seat using KeyStore
    val keystore = KeyStore.getInstance("JKS")
    keystore.load(new FileInputStream(new File("/home/mikail/Tmp/Ofsted/ofsted.jks")), password.toCharArray)
    // (v) Extract the certificate.
    val cert = keystore.getCertificate("le-externalextranetuser!00282yearsha22003template!0029-7c3ba8e1-ea15-421f-bd7a-a3cfea301c83")
    cert
  }

  override def outputFile(soapMessage : SOAPMessage) : String \/ Unit = {
    Try {
      val outputFile = new File("/home/mikail/Tmp/Ofsted/playTest.xml")
      val fos = new FileOutputStream(outputFile)
      soapMessage.writeTo(fos)
      fos.close()
    } match {
      case Success(a) => \/-(a)
      case Failure(exception) => -\/(exception.getMessage)
    }
  }


  override def call(soapMessage: SOAPMessage): String \/ Unit = Try{
    System.setProperty("javax.xml.soap.MessageFactory", "com.sun.xml.internal.messaging.saaj.soap.ver1_2.SOAPMessageFactory1_2Impl")
    System.setProperty("javax.xml.bind.JAXBContext", "com.sun.xml.internal.bind.v2.ContextFactory")
    val soapFile = new File("/home/mikail/Tmp/Ofsted/playTest.xml")
    val fis = new FileInputStream(soapFile)
    val ss = new StreamSource(fis)

    // Create a SOAP Message Object

    val msg = MessageFactory.newInstance.createMessage
    val soapPart = msg.getSOAPPart


    // Set the soapPart Content with the stream source
    soapPart.setContent(ss)

    // Create a webService connection

    val soapConnectionFactory = SOAPConnectionFactory.newInstance
    val soapConnection = soapConnectionFactory.createConnection


    // Invoke the webService.

    val soapEndpointUrl = "https://testinfogateway.ofsted.gov.uk/OnlineOfsted/GatewayOOServices.svc"
    val resp = soapConnection.call(msg, soapEndpointUrl)

    // Reading result
    resp.writeTo(System.out)

    fis.close()
    soapConnection.close()

  } match {
    case Success(value) => \/-(value)
    case Failure(exception) => -\/("Some failure in call")
  }


}