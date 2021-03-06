package $organization$.$name;format="lower,word"$

import org.deeplearning4j.scalnet.layers.{Dense, DenseOutput}
import org.deeplearning4j.scalnet.regularizers.L2
import org.deeplearning4j.scalnet.models.NeuralNet
import org.deeplearning4j.scalnet.optimizers.SGD
import org.deeplearning4j.optimize.listeners.ScoreIterationListener
import org.deeplearning4j.util.ModelSerializer
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.dataset.SplitTestAndTrain
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scopt.OptionParser

import java.io.File

case class TrainConfig(
  input: File = null,
  modelName: String = "",
  nEpochs: Int = 1
)

object TrainConfig {
  val parser = new OptionParser[TrainConfig]("Train") {
      head("$name;format="lower,word"$ Train", "1.0")

      opt[File]('i', "input")
        .required()
        .valueName("<file>")
        .action( (x, c) => c.copy(input = x) )
        .text("The file with training data.")

      opt[Int]('e', "epoch")
        .action( (x, c) => c.copy(nEpochs = x) )
        .text("Number of times to go over whole training set.")

      opt[String]('o', "output")
        .required()
        .valueName("<modelName>")
        .action( (x, c) => c.copy(modelName = x) )
        .text("Name of trained model file.")
    }

    def parse(args: Array[String]): Option[TrainConfig] = {
      parser.parse(args, TrainConfig())
    }
}

object Train {
  private val log = LoggerFactory.getLogger(getClass)

  private def net(nIn: Int, nOut: Int) = {
    val learningRate = 0.01

    val model = new NeuralNet
    model.add(new Dense(128, nIn = nIn, activation = "relu", regularizer = L2(learningRate * 0.005)))
    model.add(new Dense(128, activation = "relu", regularizer = L2(learningRate * 0.005)))
    model.add(new Dense(128, activation = "relu", regularizer = L2(learningRate * 0.005)))
    model.add(new DenseOutput(nOut, activation = "softmax", lossFunction = LossFunction.MCXENT,
      regularizer = L2(learningRate * 0.005)))
    model.compile(optimizer = SGD(learningRate))
    model
  }
    
  def main(args: Array[String]): Unit = {
    TrainConfig.parse(args) match {
      case Some(config) =>
        log.info("Starting training")

        train(config)

        log.info("Training finished.")
      case _ =>
        log.error("Invalid arguments.")
    }
  }

  private def train(c: TrainConfig): Unit = {
    val (trainData, normalizer) = DataIterators.irisCsv(c.input)

    log.info("Data Loaded")

    val model = net(4, 3)

    model.fit(iter = trainData, nbEpoch = c.nEpochs, listeners = List(new ScoreIterationListener(5)))

    ModelSerializer.writeModel(model.getNetwork, c.modelName, true)
    normalizer.save((1 to 4).map(j => new File(c.modelName + s".norm$"$"$j")):_*)

    log.info(s"Model saved to: $"$"${c.modelName}")
  }
}
