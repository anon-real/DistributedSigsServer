package models

import java.nio.charset.StandardCharsets

object RequestStatus {
  val pendingApproval = "Pending Approval"
  val rejected = "Rejected"
  val approved = "Approved"
  val paid = "Fund Paid"
}

case class Team(name: String, description: String, address: String, id: Option[Long] = None)

case class Member(pk: String, teamId: Long)

case class Request(title: String, amount: Long, description: String, address: String, teamId: Long, var status: Option[String] = Some(RequestStatus.pendingApproval), id: Option[Long] = None)

case class Commitment(pk: String, a: String, reqId: Long)

case class Transaction(reqId: Long, isPartial: Boolean, bytes: Array[Byte], isValid: Boolean, isConfirmed: Boolean) {
    override def toString: String = new String(bytes, StandardCharsets.UTF_16)
}
