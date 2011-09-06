package de.fuberlin.wiwiss.silk

import datasource.Source
import instance.{Instance, InstanceSpecification, InstanceCache}
import util.SourceTargetPair
import java.util.logging.{Level, Logger}
import util.task.{Finished, Future, Task}

/**
 * Loads the instance cache
 */
//TODO remove indexFunction argument by integrating it into instance cache
class LoadTask(sources: SourceTargetPair[Source],
               caches: SourceTargetPair[InstanceCache],
               instanceSpecs: SourceTargetPair[InstanceSpecification],
               indexFunction: Option[Instance => Set[Int]] = None) extends Task[Unit] {
  taskName = "Loading"

  @volatile var exception: Exception = null

  @volatile var sourceLoader: LoadingThread = null
  @volatile var targetLoader: LoadingThread = null

  @volatile var canceled = false

  override def execute() {
    canceled = false
    sourceLoader = new LoadingThread(true)
    targetLoader = new LoadingThread(false)

    sourceLoader.start()
    targetLoader.start()

    while ((sourceLoader.isAlive || targetLoader.isAlive) && !canceled) {
      Thread.sleep(100)
    }

    if (canceled) {
      sourceLoader.interrupt()
      targetLoader.interrupt()

      if (exception != null) {
        throw exception
      }
    }
  }

  /**
   * Executes this task in the background.
   * Returns as soon as both caches are being written.
   */
  override def runInBackground(): Future[Unit] = {
    val future = super.runInBackground()

    //Wait until the caches are being written
    while (!status.isInstanceOf[Finished] && !(caches.source.isWriting && caches.target.isWriting)) {
      Thread.sleep(100)
    }

    future
  }

  override def stopExecution() {
    canceled = true
    if(sourceLoader != null) sourceLoader.interrupt()
    if(targetLoader != null) targetLoader.interrupt()
  }

  class LoadingThread(selectSource: Boolean) extends Thread {
    private val source = sources.select(selectSource)
    private val instanceCache = caches.select(selectSource)
    private val instanceSpec = instanceSpecs.select(selectSource)

    override def run() {
      try {
        logger.info("Loading instances of dataset " + source.dataSource.toString)

        instanceCache.clear()
        instanceCache.write(source.retrieve(instanceSpec), indexFunction)
        instanceCache.close()
      } catch {
        case ex: Exception => {
          logger.log(Level.WARNING, "Error loading resources", ex)
          exception = ex
          canceled = true
        }
      }
    }
  }

}
