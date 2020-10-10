package controllers

import javax.inject._

import models._
import play.api.libs.json.Json
import play.api.mvc._
import views.EntityRW._

import scala.concurrent.{ExecutionContext, Future}

class Entities @Inject()( tables: DBTables,
                          cc: ControllerComponents
                        )(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  /**
   * The index action.
   */
  def index = Action.async {
    tables.getEntities map (seq => Ok(Json.toJson(seq)))
  }


  def show(name: String) = TODO


  def create(name: String) = TODO


  def delete(name: String) = TODO

  def reset(name: String) = TODO
}
