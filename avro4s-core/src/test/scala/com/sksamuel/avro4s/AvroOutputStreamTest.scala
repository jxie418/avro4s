package com.sksamuel.avro4s

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

import org.apache.avro.file.{DataFileReader, SeekableByteArrayInput}
import org.apache.avro.generic.{GenericDatumReader, GenericRecord}
import org.apache.avro.util.Utf8
import org.scalatest.concurrent.Timeouts
import org.scalatest.{Matchers, WordSpec}
import shapeless.Lazy

import scala.collection.JavaConverters._

class AvroOutputStreamTest extends WordSpec with Matchers with Timeouts {

  def read[T](out: ByteArrayOutputStream)(implicit schema: Lazy[AvroSchema[T]]): GenericRecord = read(out.toByteArray)
  def read[T](bytes: Array[Byte])(implicit schema: Lazy[AvroSchema[T]]): GenericRecord = {
    val datum = new GenericDatumReader[GenericRecord](schema.value.apply)
    val reader = new DataFileReader[GenericRecord](new SeekableByteArrayInput(bytes), datum)
    reader.hasNext
    reader.next()
  }

  "AvroOutputStream" should {
    "support java enums" in {
      case class Test(wine: Wine)

      val output = new ByteArrayOutputStream
      val avro = AvroOutputStream[Test](output)
      avro.write(Test(Wine.Malbec))
      avro.close()

      val record = read[Test](output)
      record.get("wine").toString shouldBe Wine.Malbec.name
    }
    "write big decimal" in {
      case class Test(dec: BigDecimal)

      val output = new ByteArrayOutputStream
      val avro = AvroOutputStream[Test](output)
      avro.write(Test(123.456789))
      avro.close()

      val record = read[Test](output)
      new String(record.get("dec").asInstanceOf[ByteBuffer].array) shouldBe "123.456789"
    }
    "write out strings" in {
      case class Test(str: String)

      val output = new ByteArrayOutputStream
      val avro = AvroOutputStream[Test](output)
      avro.write(Test("sammy"))
      avro.close()

      val record = read[Test](output)
      record.get("str").toString shouldBe "sammy"
    }
    "write out booleans" in {
      case class Test(bool: Boolean)

      val output = new ByteArrayOutputStream
      val avro = AvroOutputStream[Test](output)
      avro.write(Test(true))
      avro.close()

      val record = read[Test](output)
      record.get("bool").toString shouldBe "true"
    }
    "write out longs" in {
      case class Test(l: Long)

      val output = new ByteArrayOutputStream
      val avro = AvroOutputStream[Test](output)
      avro.write(Test(56l))
      avro.close()

      val record = read[Test](output)
      record.get("l").toString shouldBe "56"
    }
    "write out ints" in {
      case class Test(i: Int)

      val output = new ByteArrayOutputStream
      val avro = AvroOutputStream[Test](output)
      avro.write(Test(666))
      avro.close()

      val record = read[Test](output)
      record.get("i").toString shouldBe "666"
    }
    "write out doubles" in {
      case class Test(d: Double)

      val output = new ByteArrayOutputStream
      val avro = AvroOutputStream[Test](output)
      avro.write(Test(123.456))
      avro.close()

      val record = read[Test](output)
      record.get("d").toString shouldBe "123.456"
    }
    "write out eithers of primitives for lefts" in {
      case class Test(e: Either[String, Double])

      val output = new ByteArrayOutputStream
      val avro = AvroOutputStream[Test](output)
      avro.write(Test(Left("sam")))
      avro.close()

      val record = read[Test](output)
      record.get("e").toString shouldBe "sam"
    }
    "write out eithers of primitives for rights" in {
      case class Test(e: Either[String, Double])

      val output = new ByteArrayOutputStream
      val avro = AvroOutputStream[Test](output)
      avro.write(Test(Right(45.4d)))
      avro.close()

      val record = read[Test](output)
      record.get("e").toString shouldBe "45.4"
    }
    "write a Some as populated union" in {
      case class Test(opt: Option[Double])

      val output = new ByteArrayOutputStream
      val avro = AvroOutputStream[Test](output)
      avro.write(Test(Some(123.456d)))
      avro.close()

      val record = read[Test](output)
      record.get("opt").toString shouldBe "123.456"
    }
    "write a None as union null" in {
      case class Test(opt: Option[Double])

      val output = new ByteArrayOutputStream
      val avro = AvroOutputStream[Test](output)
      avro.write(Test(None))
      avro.close()

      val record = read[Test](output)
      record.get("opt") shouldBe null
    }
    "write Array of primitives" in {
      case class Test(array: Array[Double])

      val output = new ByteArrayOutputStream
      val avro = AvroOutputStream[Test](output)
      avro.write(Test(Array(1d, 2d, 3d, 4d)))
      avro.close()

      val record = read[Test](output)
      record.get("array").asInstanceOf[java.util.List[Double]].asScala shouldBe Seq(1d, 2d, 3d, 4d)
    }
    "write Seq of primitives" in {
      case class Test(sequence: Seq[Double])

      val output = new ByteArrayOutputStream
      val avro = AvroOutputStream[Test](output)
      avro.write(Test(Seq(1d, 2d, 3d, 4d)))
      avro.close()

      val record = read[Test](output)
      record.get("sequence").asInstanceOf[java.util.List[Double]].asScala shouldBe Seq(1d, 2d, 3d, 4d)
    }
    "write Seq of nested classes" in {
      case class Nested(str: String, boolean: Boolean)
      case class Test(seq: Seq[Nested])

      val output = new ByteArrayOutputStream
      val avro = AvroOutputStream[Test](output)
      avro.write(Test(List(Nested("sam", true), Nested("ham", false))))
      avro.close()

      val record = read[Test](output)
      val data = record.get("seq").asInstanceOf[java.util.List[GenericRecord]].asScala.toList
      data.head.get("str").toString shouldBe "sam"
      data.last.get("str").toString shouldBe "ham"
    }
    "write Set of primitives" in {
      case class Test(set: Set[Double])

      val output = new ByteArrayOutputStream
      val avro = AvroOutputStream[Test](output)
      avro.write(Test(Set(1d, 9d, 9d, 9d, 9d)))
      avro.close()

      val record = read[Test](output)
      record.get("set").asInstanceOf[java.util.List[Double]].asScala.toSet shouldBe Set(1d, 9d)
    }
    "write Set of nested classes" in {
      case class Nested(str: String, boolean: Boolean)
      case class Test(set: Set[Nested])

      val set = Set(Nested("sam", true), Nested("ham", false))

      val output = new ByteArrayOutputStream
      val avro = AvroOutputStream[Test](output)
      avro.write(Test(set))
      avro.close()

      val record = read[Test](output)
      val actual = record.get("set").asInstanceOf[java.util.List[GenericRecord]].asScala.toSet
      actual.map(_.get("str").toString) shouldBe Set("sam", "ham")
      actual.map(_.get("boolean").toString.toBoolean) shouldBe Set(true, false)
    }
    "write list of primitives" in {
      case class Test(list: List[Double])

      val output = new ByteArrayOutputStream
      val avro = AvroOutputStream[Test](output)
      avro.write(Test(List(1d, 2d, 3d, 4d)))
      avro.close()

      val record = read[Test](output)
      record.get("list").asInstanceOf[java.util.List[Double]].asScala shouldBe List(1d, 2d, 3d, 4d)
    }
    "write map of strings" in {
      case class Test(map: Map[String, String])

      val output = new ByteArrayOutputStream
      val avro = AvroOutputStream[Test](output)
      avro.write(Test(Map("name" -> "sammy")))
      avro.close()

      val record = read[Test](output)
      record.get("map").asInstanceOf[java.util.Map[Utf8, Utf8]].asScala.map { case (k, v) =>
        k.toString -> v.toString
      } shouldBe Map("name" -> "sammy")
    }
    "write map of doubles" in {
      case class Test(map: Map[String, Double])

      val output = new ByteArrayOutputStream
      val avro = AvroOutputStream[Test](output)
      avro.write(Test(Map("name" -> 12.3d)))
      avro.close()

      val record = read[Test](output)
      record.get("map").asInstanceOf[java.util.Map[Utf8, java.lang.Double]].asScala.map { case (k, v) =>
        k.toString -> v.toString.toDouble
      } shouldBe Map("name" -> 12.3d)
    }
    "write map of booleans" in {
      case class Test(map: Map[String, Boolean])

      val output = new ByteArrayOutputStream
      val avro = AvroOutputStream[Test](output)
      avro.write(Test(Map("name" -> true)))
      avro.close()

      val record = read[Test](output)
      record.get("map").asInstanceOf[java.util.Map[Utf8, java.lang.Boolean]].asScala.map { case (k, v) =>
        k.toString -> v.toString.toBoolean
      } shouldBe Map("name" -> true)
    }
    "write map of nested classes" in {
      case class Nested(str: String)
      case class Test(map: Map[String, Nested])

      val output = new ByteArrayOutputStream
      val avro = AvroOutputStream[Test](output)
      avro.write(Test(Map("foo" -> Nested("sam"))))
      avro.close()

      val record = read[Test](output)
      val map = record.get("map").asInstanceOf[java.util.Map[Utf8, GenericRecord]].asScala.map { case (k, v) =>
        k.toString -> v
      }
      map("foo").get("str").toString shouldBe "sam"
    }
    "support extends AnyVal" in {
      val instance = ValueWrapper(ValueClass("bob"))

      val output = new ByteArrayOutputStream
      val avro = AvroOutputStream[ValueWrapper](output)
      avro.write(instance)
      avro.close()

      val record = read[ValueWrapper](output)
      record.get("valueClass").asInstanceOf[GenericRecord].get("value").toString shouldBe "bob"
    }
//    "support traits" in {
//      val instance = Traits(A("foo"))
//
//      val output = new ByteArrayOutputStream
//      val avro = AvroOutputStream[Traits](output)
//      avro.write(instance)
//      avro.close()
//
//      val record = read[Traits](output)
//      record.get("str").toString shouldBe "foo"
//    }
    //    "support sealed traits" in {
    //
    //      val instance = Seals(KissFromARose, Crazy("foo"))
    //
    //      val output = new ByteArrayOutputStream
    //      val avro = AvroOutputStream[Seals](output)
    //      avro.write(instance)
    //      avro.close()
    //
    //      val record = read[Seals](output)
    //      record.get("seal1").toString shouldBe "bob"
    //      record.get("seal2").toString shouldBe "bob"
    //    }
  }
}

trait Trait

case class A(str: String) extends Trait

case class Traits(t: Trait)

trait Seal

case object KissFromARose extends Seal

case class Crazy(v: String) extends Seal

case class Seals(seal1: Seal, seal2: Seal)

case class ValueWrapper(valueClass: ValueClass)

case class ValueClass(value: String) extends AnyVal