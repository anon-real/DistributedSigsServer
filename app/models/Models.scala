package models

import java.nio.charset.StandardCharsets

object RequestStatus {
  val pendingApproval = "Pending Approval"
  val rejected = "Rejected"
  val approved = "Approved"
  val paid = "Fund Paid"
}

case class Team(name: String, description: String, address: String, id: Option[Long] = None) {
  def toJson: String = {
    val desc = description.replace("\n", "\\n")
    s"""{
      |  "name": "$name",
      |  "description": "$desc",
      |  "address": "$address",
      |  "id": ${id.get}
      |}""".stripMargin
  }
}

case class Member(pk: String, nickName: String, teamId: Long, id: Option[Long] = None) {
  def toJson: String = {
    s"""{
      |  "pk": "$pk",
      |  "teamId": $teamId,
      |  "nickName": "$nickName",
      |  "id": ${id.get}
      |}""".stripMargin
  }

}

case class Request(title: String, amount: Double, description: String, address: String, teamId: Long, var txId: Option[String], var status: Option[String] = Some(RequestStatus.pendingApproval), id: Option[Long] = None) {
  def toJson(commitments: String = ""): String = {
    val desc = description.replaceAll("\r\n", "\\\\n")
    s"""{
       |  "title": "$title",
       |  "amount": $amount,
       |  "description": "$desc",
       |  "address": "$address",
       |  "teamId": $teamId,
       |  "status": "${status.get}",
       |  "id": ${id.get},
       |  "commitments": [$commitments]
       |}""".stripMargin
  }

}

// this model doesn't need pk since it is stored in member table. but anyway we store it here too to have easy access to it.
case class Commitment(memberId: Long, a: String, reqId: Long) {
  def toJson: String = {
    s"""{
       |  "memberId": $memberId,
       |  "a": "$a",
       |  "requestId": $reqId
       |}""".stripMargin
  }

  def toJson(mem: Member): String = {
    s"""{
       |  "memberId": $memberId,
       |  "a": "$a",
       |  "member": ${mem.toJson},
       |  "requestId": $reqId
       |}""".stripMargin
  }
}

case class Proof(memberId: Long, reqId: Long, proof: String, simulated: Boolean) {
  def toJson: String = {
    s"""{
       |  "memberId": $memberId,
       |  "requestId": $reqId,
       |  "proof": $proof,
       |  "simulated": $simulated
       |}""".stripMargin
  }
}

case class Transaction(reqId: Long, bytes: Array[Byte]) {
  override def toString: String = new String(bytes, StandardCharsets.UTF_16)

  def toJson : String = {
    s"""{
      |  "tx": $toString,
      |  "requestId": $reqId
      |}""".stripMargin
  }
}
