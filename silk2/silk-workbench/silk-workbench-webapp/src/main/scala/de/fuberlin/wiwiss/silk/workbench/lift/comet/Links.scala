package de.fuberlin.wiwiss.silk.workbench.lift.comet

import net.liftweb.http.js.{JsCmd, JsCmds}
import de.fuberlin.wiwiss.silk.output.Link
import net.liftweb.http.js.JsCmds.{OnLoad, SetHtml, Script}
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.workbench.lift.util.{PrefixRegistry, JS}
import xml.{Text, NodeSeq}
import net.liftweb.http.{SHtml, CometActor}
import net.liftweb.http.js.JE.{Call, JsRaw}
import de.fuberlin.wiwiss.silk.workbench.evaluation._
import de.fuberlin.wiwiss.silk.entity.{Path, Entity}
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import java.util.logging.Logger

/**
 * A widget which displays a list of links.
 */
trait Links extends CometActor {
  protected def linkingTask = User().linkingTask

  /**The number of links shown on one page */
  private val pageSize = 100

  protected val showStatus = true

  protected val showButtons = true

  protected val showDetails = true

  protected val showEntities = false

  protected def registerEvents() {}

  protected def links: Seq[EvalLink]

  protected def renderStatus(link: EvalLink): NodeSeq = NodeSeq.Empty

  protected def renderButtons(link: EvalLink): NodeSeq = NodeSeq.Empty

  /**Prefixes used to shorten URIs. We use known prefixes from the global registry and from the project */
  private var prefixes = PrefixRegistry.all ++ User().project.config.prefixes

  override protected val dontCacheRendering = true

  override def render = {
    registerEvents()

    prefixes = PrefixRegistry.all ++ User().project.config.prefixes

    val showLinksFunc = JsCmds.Function("showLinks", "page" :: Nil, SHtml.ajaxCall(JsRaw("page"), (pageStr) => showLinks(pageStr.toInt))._2.cmd)

    bind("entry", defaultHtml,
         "script" -> Script(OnLoad(updateLinksCmd) & showLinksFunc),
         "filter" -> <div id="filter">Filter:{JS.ajaxLiveText(LinkFilter(), applyFilter)}</div>,
         "list" -> <div id="results" />
    )
  }

  protected def updateLinksCmd: JsCmd = {
    JsRaw("initPagination(" + links.size + ");").cmd
  }

  private def showLinks(page: Int) = JS.Try("show links") {
    val html =
      <div>
        <div class="link">
          <div class="link-header heading">
            <div class="link-source">Source: <span class="source-value">{linkingTask.linkSpec.datasets.source.sourceId}</span></div>
            <div class="link-target">Target: <span class="target-value">{linkingTask.linkSpec.datasets.target.sourceId}</span></div>
            <div class="link-confidence">{renderConfidenceHeader}</div>
            { if(showStatus) <div class="link-status"><span>Status</span></div> else NodeSeq.Empty }
            { if(showButtons) <div class="link-buttons"><span>Correct?</span></div> else NodeSeq.Empty }
          </div>
        </div> {
          val filteredLinks = LinkFilter.filter(links)
          val sortedLinks = LinkSorter.sort(filteredLinks)
          var counter = 1
          for(link <- sortedLinks.view(page * pageSize, (page + 1) * pageSize)) yield {
            counter = counter + 1
            renderLink(link, counter)
          }
        }
      </div>

    SetHtml("results", html) & Call("initTrees").cmd & Call("updateResultsWidth").cmd
  }

  private def renderConfidenceHeader = {
    def sort() = {
      if (LinkSorter() == ConfidenceSorterDescending) {
        LinkSorter() = ConfidenceSorterAscending
      } else {
        LinkSorter() = ConfidenceSorterDescending
      }
      updateLinksCmd
    }

    val icon = LinkSorter() match {
      case ConfidenceSorterAscending => "./static/img/sort-ascending.png"
      case ConfidenceSorterDescending => "./static/img/sort-descending.png"
      case _ => "./static/img/sort.png"
    }

    SHtml.a(sort _, <span>Score<img src={icon}/></span>)
  }

  private def applyFilter(value: String) = {
    LinkFilter() = value
    JsRaw("useFilter(" + links.size + ");").cmd
  }

  /**
   * Renders a link.
   *
   * @param link The link to be rendered
   */
  private def renderLink(link : EvalLink, counter : Int) = {
    <div class="link" id={getId(link)} >
      <div class={if (counter%2==0) "link-header grey" else "link-header" } onmouseover="$(this).addClass('link-over');" onmouseout="$(this).removeClass('link-over');">
        <div id={getId(link, "toggle")}><span class="ui-icon ui-icon ui-icon-triangle-1-e"></span></div>
        <div class="link-source"><a href={link.source} target="_blank">{prefixes.shorten(link.source)}</a></div>
        <div class="link-target"><a href={link.target} target="_blank">{prefixes.shorten(link.target)}</a></div>
        <div class="link-confidence">{renderConfidence(link)}</div>
        { if(showStatus) <div class="link-status">{ renderStatus(link) }</div> else NodeSeq.Empty }
        { if(showButtons) <div class="link-buttons">{ renderButtons(link) }</div> else NodeSeq.Empty }

      </div>
      <div class="link-details" id={getId(link, "details")}>
        { if(showDetails) renderDetails(link.details) else NodeSeq.Empty }
        { if(showEntities) renderEntities(link.entities) else NodeSeq.Empty }
      </div>
      <div style="clear:both"></div>
    </div>
  }

  private def renderConfidence(link: Link): NodeSeq = link.details match {
    case None => <div class="confidencebar">Pending...</div>
    case Some(sim) => {
      <div class="confidencebar">
        <div class="confidence">{"%.1f".format(sim.value.getOrElse(-1.0) * 100)}%</div>
      </div>
    }
  }

  private def renderEntities(entities: Option[SourceTargetPair[Entity]]) = {
    entities match {
      case Some(SourceTargetPair(sourceEntity, targetEntity)) => {
        <ul class="details-tree">
          { renderEntity(sourceEntity, "source") }
          { renderEntity(targetEntity, "target") }
        </ul>
      }
      case None => Text("No properties loaded")
    }
  }

  private def renderEntity(entity: Entity, divClassPrefix: String) = {
    <li>
      <span class={divClassPrefix+"-value"}>{ entity.uri }</span>
      <ul>
        { for((path, index) <- entity.desc.paths.zipWithIndex) yield renderValues(path, entity.evaluate(index), divClassPrefix) }
      </ul>
    </li>
  }

  private def renderValues(path: Path, values: Set[String], divClassPrefix: String) = {
    val firstValues = values.take(11)
    <li>
      { path.serialize }
      { firstValues.map(v => <span class={divClassPrefix+"-value"}>{v}</span>) }
      { if(firstValues.size > 10) "..." else NodeSeq.Empty }
    </li>
  }

  private def renderDetails(details: Option[Link.Confidence]): NodeSeq = {
    details match {
      case Some(similarity) => {
        <ul class="details-tree">
        { renderSimilarity(similarity) }
        </ul>
      }
      case None => Text("No details")
    }
  }

  private def renderSimilarity(similarity: Link.Confidence): NodeSeq = similarity match {
    case Link.AggregatorConfidence(value, aggregation, children) => {
      <li>
        <span class="aggregation">Aggregation: {aggregation.aggregator.pluginId} ({aggregation.id})</span>{ renderConfidence(value) }
          <ul>
            { children.map(renderSimilarity) }
          </ul>
      </li>
    }
    case Link.ComparisonConfidence(value, comparison, input1, input2) => {
      <li>
        <span class="comparison">Comparison: {comparison.metric.pluginId} ({comparison.id})</span>{ renderConfidence(value) }
          <ul>
            { renderInputValue(input1, "source") }
            { renderInputValue(input2, "target") }
          </ul>
      </li>
    }
    case Link.SimpleConfidence(value) => {
      <li>Link Specification is empty</li>
    }
  }

  private def renderConfidence(value : Option[Double]) = value match {
    case Some(v) => <div class="confidencebar"><div class="confidence">{"%.1f".format((v) * 100)}%</div></div>
    case None => NodeSeq.Empty
  }

  private def renderInputValue(value : Link.InputValue, divClassPrefix : String) = {
    <li>
      <span class="input">Input ({value.input.id})<span class={divClassPrefix+"-path"}>{value.input.path.serialize}</span>{value.values.map(v => <span class={divClassPrefix+"-value"}>{v}</span>) }</span>
    </li>
  }

  protected def getId(link: Link, prefix: String = "") = {
    prefix + link.hashCode
  }
}
