package controllers

import javax.inject._

import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.i18n._
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class Entities @Inject()( //repo: EntitiesRepository,
                          cc: MessagesControllerComponents
                                )(implicit ec: ExecutionContext)
  extends MessagesAbstractController(cc) {

  /**
   * The index action.
   */
  def index = TODO


  def show(name: String) = TODO


  def create(name: String) = TODO


  def delete(name: String) = TODO

  def reset(name: String) = TODO
}
