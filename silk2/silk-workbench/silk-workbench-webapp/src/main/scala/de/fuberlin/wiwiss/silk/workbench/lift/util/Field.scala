package de.fuberlin.wiwiss.silk.workbench.lift.util

import xml.NodeSeq
import net.liftweb.http.js.JsCmd
import net.liftweb.http.SHtml
import net.liftweb.common.Empty
import java.util.UUID
import net.liftweb.http.js.JE.JsRaw

/**
 * An input field.
 */
sealed trait Field[T] {
  val label: String

  val description: String

  var value: T

  /**The id of this field */
  protected lazy val id: String = UUID.randomUUID.toString

  def render: NodeSeq

  def updateValueCmd: JsCmd
}

/**
 * A field which holds a string value
 */
case class StringField(label: String, description: String, initialValue: () => String = (() => "")) extends Field[String] {
  override var value = initialValue()

  override def render = SHtml.text(value, value = _, "id" -> id, "size" -> "60", "title" -> description)

  override def updateValueCmd = {
    value = initialValue()
    JsRaw("$('#" + id + "').val('" + value + "');").cmd
  }
}

/**
 * A field which holds an integer value
 */
case class IntField(label: String, description: String, min: Int, max: Int, initialValue: () => Int = (() => 0)) extends Field[Int] {
  override var value = initialValue()

  override def render = SHtml.number(value, value = _, min, max, "id" -> id, "size" -> "60", "title" -> description)

  override def updateValueCmd = {
    value = initialValue()
    JsRaw("$('#" + id + "').val('" + value + "');").cmd
  }
}

/**
 * A field which holds a string value
 */
case class BooleanField(label: String,
                        description: String,
                        initialValue: () => Boolean = (() => false),
                        onUpdate: Boolean => JsCmd = (_ => JS.Empty)) extends Field[Boolean] {

  override var value = initialValue()

  override def render = {
    SHtml.ajaxCheckbox(value, updateValue _, "id" -> id, "size" -> "60", "title" -> description, "id" -> id) ++
    <label for={id}>Enable</label>
  }

  override def updateValueCmd = {
    value = initialValue()
    JsRaw("$('#" + id + "').val('" + value + "');").cmd
  }

  def enableCmd(enable: Boolean) = {
    if(enable)
      JsRaw("$('#" + id + "').removeAttr('disabled')").cmd
    else
      JsRaw("$('#" + id + "').attr('disabled', 'disabled')").cmd
  }

  private def updateValue(newValue: Boolean) = {
    value = newValue
    onUpdate(newValue)
  }
}

/**
 * A field which holds an enumeration of values which can be selected.
 */
case class SelectField(label: String, description: String, allowedValues: () => Seq[String], initialValue: () => String) extends Field[String] {
  override var value = initialValue()

  override def render = SHtml.untrustedSelect(Nil, Empty, value = _, "id" -> id, "title" -> description)

  override def updateValueCmd = {
    value = initialValue()

    //Generate the options of the select box
    val options =
      for(allowedValue <- allowedValues()) yield {
        if(allowedValue == value)
          <option value={allowedValue} selected="true">{allowedValue}</option>
        else
          <option value={allowedValue}>{allowedValue}</option>
      }

    //Update select box
    JsRaw("$('#" + id + "').children().remove();").cmd &
      JsRaw("$('#" + id + "').append('" + options.mkString + "');").cmd
  }
}

