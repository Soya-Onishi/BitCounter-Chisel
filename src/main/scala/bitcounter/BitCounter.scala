package bitcounter

import chisel3._
import chisel3.util._

import scala.annotation.tailrec

class BitCounter(width: Int, stepsPerCycle: Int) extends Module {
  assert(width > 0, s"width must be larger than 0, but $width")

  def atLeastWidth(n: Int): Int = {
    val logged = math.log(n + 1) / math.log(2)
    math.ceil(logged).toInt
  }

  def constructStep(bits: Vector[Bool]): (Vector[Bool], Vector[Bool]) = {
    def identity(bits: Vector[Bool]): (Vector[Bool], Vector[Bool]) = (bits, Vector.empty)
    def applyHA(bits: Vector[Bool]): (Vector[Bool], Vector[Bool]) = {
      val ha = Module(new HA)
      ha.io.a := bits(0)
      ha.io.b := bits(1)

      val out = Vector(ha.io.out)
      val cout = Vector(ha.io.cout)

      (out, cout)
    }

    def applyCSA(xs: Vector[Bool]): (Vector[Bool], Vector[Bool]) = {
      val csa = Module(new CSA)
      csa.io.in := VecInit(xs)

      val out = Vector(csa.io.out)
      val cout = Vector(csa.io.cout)
      (out, cout)
    }

    val bitss = bits.sliding(3, 3).map(bits => bits.length match {
      case 0 => throw new Exception("length is 0")
      case 1 => identity(bits)
      case 2 => applyHA(bits)
      case 3 => applyCSA(bits)
    })

    val (outss, coutss) = bitss.toVector.unzip
    (outss.flatten, coutss.flatten)
  }

  def constructCounter(bits: Vector[Bool]): (UInt, Bool, Bool) = {
    def formatBitsToUInt(bits: Vector[Bool]): UInt = {
      bits.map(_.asUInt).reduceLeft[UInt]{ case (acc, bit) => Cat(acc, bit.asUInt()) }
    }

    def loop(bitss: Vector[Vector[Bool]], remainSteps: Int, beforeValid: Bool): (UInt, Bool, Bool) = {
      def component(b: Bool): Bool =
        if(remainSteps != 1) WireInit(b)
        else RegInit(false.B)

      val (outss, carriess) = bitss.map(bits => constructStep(bits)).unzip
      val nextss0 = ((Vector.empty[Bool] +: outss) zip (carriess :+ Vector.empty[Bool])).map { case (cs, bs) => cs ++ bs }
      val nextss =
        if(nextss0.head.isEmpty) nextss0.tail
        else nextss0
      val nextStep =
        if(remainSteps == 1) stepsPerCycle
        else math.max(remainSteps - 1, 0)

      if(nextss.forall(_.length == 1)) (formatBitsToUInt(nextss.flatten), false.B, beforeValid)
      else {
        val components = nextss.map(_.map(component))
        val nextValid = component(beforeValid)
        val (count, valid, lastValid) = loop(components, nextStep, nextValid)

        if(remainSteps == 1) {
          val latchFlag = WireInit(!valid | (valid & io.out.ready))
          (components.flatten zip nextss.flatten).foreach{ case (reg, bit) => reg := Mux(latchFlag, bit, reg) }
          nextValid := Mux(latchFlag, beforeValid, nextValid)
        }

        val regValid =
          if(remainSteps == 1) nextValid
          else                 valid

        (count, regValid, lastValid)
      }
    }

    loop(Vector(bits), stepsPerCycle, io.in.valid)
  }

  val io = IO(new Bundle{
    val in  = Flipped(DecoupledIO(Vec(width, Bool())))
    val out = DecoupledIO(UInt(atLeastWidth(width).W))
  })

  val (count, valid, lastValid) = constructCounter(io.in.bits.toVector)
  io.in.ready := !valid | (valid & io.out.ready)
  io.out.valid := lastValid
  io.out.bits := count
}

class CSA extends Module {
  val io = IO(new Bundle {
    val in = Input(Vec(3, Bool()))
    val out = Output(Bool())
    val cout = Output(Bool())
  })

  val ha = Vector.fill(2)(Module(new HA))
  ha(0).io.a := io.in(0)
  ha(0).io.b := io.in(1)
  ha(1).io.a := ha(0).io.out
  ha(1).io.b := io.in(2)

  io.out := ha(1).io.out
  io.cout := ha(0).io.cout | ha(1).io.cout
}

class HA extends Module {
  val io = IO(new Bundle {
    val a = Input(Bool())
    val b = Input(Bool())
    val out = Output(Bool())
    val cout = Output(Bool())
  })

  io.out := io.a ^ io.b
  io.cout := io.a & io.b
}


