package io.caerulean.sipstack

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

import scala.util.Try

/**
* Created by jmenga on 5/12/14.
*/
object JsonUtils {
  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)

  def toJson(value: Map[Symbol, Any]): Try[String] = toJson(value map { case (k,v) => k.name -> v})
  def toJson(value: Any): Try[String] = Try(mapper.writeValueAsString(value))
  def toMap[V](json:String)(implicit m: Manifest[V]) = fromJson[Map[String,V]](json)

  /**
   * Serializes to type T with exception handling
   * Recommended for normal use
   * @param json
   * @param m
   * @tparam T
   * @return
   */
  def fromJson[T](json: String)(implicit m : Manifest[T]): Try[T] = Try(mapper.readValue[T](json))

  def fromJson[T](option: Option[String])(implicit m: Manifest[T]) : Option[T] = {
    for {
      json <- option
      value <- fromJson[T](json).toOption
    } yield value
  }
}


