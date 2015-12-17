/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.util

case class Table(name: String, header: Seq[String], rows: Seq[String], values: Seq[Seq[Any]]) {

  def transpose = Table(name, rows, header, values.transpose)

  /**
   * Formats this table as CSV.
   */
  def toCsv = {
    val csv = new StringBuilder()

    csv.append(name + "," + header.mkString(",") + "\n")
    for((label, row) <- rows zip values)
      csv.append(label + "," + row.mkString(",") + "\n")

    csv.toString
  }

  /**
   * Formats this table as textile.
   */
  def toTextile = {
    val sb = new StringBuilder()

    sb.append(header.mkString("|_. ", " |_. ", " |\n"))
    for((label, row) <- rows zip values) {
      sb.append("| " + label + " | " + row.mkString(" | ") + " |\n")
    }

    sb.toString
  }

  /**
   * Formats this table as markdown.
   */
  def toMarkdown = {
    val sb = new StringBuilder()

    sb.append(header.mkString("| ", " | ", " |\n"))
    sb.append("| " + (" --- |" * header.size) + "\n")
    for((label, row) <- rows zip values) {
      // If there are line breaks in a value, we need to generate multiple rows
      val rowLines = row.map(_.toString.split("[\n\r]+"))
      val maxLines = rowLines.map(_.length).max

      for(index <- 0 until maxLines) {
        val lineLabel = if(index == 0) label else ""
        val lineValues = rowLines.map(lines => if(index >= lines.length) "" else lines(index))
        sb.append("| " + lineLabel + " | " + lineValues.mkString(" | ") + " |\n")
      }
    }

    sb.toString
  }

  // TODO can be deleted if not needed anymore
  def toPandocMarkdown = {
    val sb = new StringBuilder()

    val functionColumnSize = 20
    val nameColumnSize = 20
    val descriptionColumnSize = 80

    sb ++= "-" * (functionColumnSize + nameColumnSize + descriptionColumnSize)
    sb ++= "\n"

    sb ++= "Function".padTo(functionColumnSize, ' ')
    sb ++= "Id".padTo(nameColumnSize, ' ')
    sb ++= "Description".padTo(descriptionColumnSize , ' ')
    sb ++= "\n"

    sb ++= "-" * (functionColumnSize - 1) + " "
    sb ++= "-" * (nameColumnSize - 1) + " "
    sb ++= "-" * descriptionColumnSize
    sb ++= "\n"

    for((label, row) <- rows zip values) {
      val functionLines = label.toString.grouped(functionColumnSize - 1).toSeq
      val idLines = row(0).toString.grouped(nameColumnSize - 1).toSeq
      val descriptionLines = row(1).toString.split("[\n\r]+").flatMap(_.grouped(descriptionColumnSize - 1)).toSeq

      val maxLines = Seq(functionLines.size, idLines.size, descriptionLines.size).max

      for(index <- 0 until maxLines) {
        val functionLine = if(index >= functionLines.size) "" else functionLines(index)
        val idLine = if(index >= idLines.size) "" else idLines(index)
        val descriptionLine = if(index >= descriptionLines.size) "" else descriptionLines(index)

        sb ++= functionLine.padTo(functionColumnSize, ' ')
        sb ++= idLine.padTo(nameColumnSize, ' ')
        sb ++= descriptionLine.padTo(descriptionColumnSize, ' ')
        sb ++= "\n"
      }

      sb ++= "\n"
    }

    sb ++= "-" * (functionColumnSize + nameColumnSize + descriptionColumnSize)
    sb ++= "\n"

    sb.toString
  }

  /**
   * Formats this table as latex.
   */
  def toLatex = {
    val sb = new StringBuilder()

    sb.append("\\begin{table}\n")
    sb.append("\\begin{tabular}{|l|" + header.map(_ => "c").mkString("|") + "|}\n")
    sb.append("\\hline\n")
    sb.append(" & " + header.mkString(" & ") + "\\\\\n")
    sb.append("\\hline\n")
    for((label, row) <- rows zip values)
      sb.append(label + " & " + row.mkString(" & ") + "\\\\\n")
    sb.append("\\hline\n")
    sb.append("\\end{tabular}\n")
    sb.append("\\caption{" + name + "}\n")
    sb.append("%\\label{}\n")
    sb.append("\\end{table}\n")

    sb.toString
  }
}