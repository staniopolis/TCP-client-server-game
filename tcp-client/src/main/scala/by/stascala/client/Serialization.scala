package by.stascala.client

import akka.util.ByteString

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

trait Serialization {
  def serialise(value: Any): Array[Byte] = {
    val stream: ByteArrayOutputStream = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(stream)
    oos.writeObject(value)
    oos.close()
    stream.toByteArray
  }

  def deSerialise(byteStr: ByteString): Any = {
    val ois = new ObjectInputStream(new ByteArrayInputStream(byteStr.toArray))
    val value = ois.readObject
    ois.close()
    value
  }

}
