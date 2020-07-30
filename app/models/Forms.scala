package models

import play.api.data.Form
import play.api.data.Forms._

object Forms {
  val teamForm = Form(
    mapping(
      "name" -> text,
      "description"  -> text,
      "address" -> text,
      "id" -> optional(longNumber)
    )(Team.apply)(Team.unapply)
  )
}
