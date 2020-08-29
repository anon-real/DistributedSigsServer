package models

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._


object Forms {
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
