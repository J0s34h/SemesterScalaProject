package com.fayzullin.yusuf.domain

import arAssociations.ArAssociation
import users.User

sealed trait ValidationError extends Product with Serializable
case class ArAssociationAlreadyExists(association: ArAssociation) extends ValidationError
case object ArAssociationNotFoundError extends ValidationError
case object OrderNotFoundError extends ValidationError
case object UserNotFoundError extends ValidationError
case class UserAlreadyExistsError(user: User) extends ValidationError
case class UserAuthenticationFailedError(userName: String) extends ValidationError
