package com.cargo

import com.cargo.api.generated.definitions.dto.{CarOfferRes, Feature, Point}
import io.scalaland.chimney.dsl._
import cats.syntax.option._
import com.cargo.model.CarOffer

package object api {
  def parseToken(bearerToken: String): String = bearerToken.drop(7)

  def mapCarOffer: PartialFunction[CarOffer, CarOfferRes] =
    _.into[CarOfferRes]
      .withFieldComputed(_.id, _.id.value.toString.some)
      .withFieldComputed(_.ownerId, _.ownerId.value.toString.some)
      .withFieldComputed(_.features, _.features.flatMap(Feature.from).toVector)
      .withFieldComputed(_.point, o => o.geolocation.map(p => Point(p.lat.toString, p.lon.toString)))
      .transform
}
