package models

object RequestStatus {
  val pendingApproval = "Pending Approval"
  val rejected = "Rejected" // currently, there is no way to find out if a request is rejected (unless all members reject it!)
  val approved = "Approved"
  val paid = "Fund Paid"
}

case class Team(name: String, description: String, address: String, id: Option[Long] = None)

case class Member(pk: String, teamId: Long)

case class Request(title: String, amount: Long, description: String, address: String, teamId: Long, var status: Option[String] = Some(RequestStatus.pendingApproval), id: Option[Long] = None)

case class Commitment(pk: String, a: String, request_id: Long)
