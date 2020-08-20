package models

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._


object Forms {
  val teamForm: Form[Team] = Form(
    mapping(
      "name" -> text,
      "description"  -> text,
      "address" -> text,
      "id" -> optional(longNumber)
    )(Team.apply)(Team.unapply)
  )

  val requestForm: Form[Request] = Form(
    mapping(
      "title" -> text,
      "amount" -> of[Double],
      "description"  -> text,
      "address" -> text,
      "team_id" -> longNumber,
      "confirmed_tx_id" -> optional(text),
      "status" -> optional(text),
      "id" -> optional(longNumber)
    )(Request.apply)(Request.unapply)
  )
}
