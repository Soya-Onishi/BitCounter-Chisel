package bitcounter

import chisel3.iotesters.{PeekPokeTester, ChiselFlatSpec, Driver}
import scala.util.Random

class BitCounterTester(counter: BitCounter, width: Int) extends PeekPokeTester(counter) {
  val random = new Random(0)
  def next: BigInt = BigInt(width, random)
  def count(value: BigInt): Int = {
    (0 until width).foldLeft(0) {
      case (acc, shamt) =>
        if(((value >> shamt) & 1) == 1) acc + 1
        else acc
    }
  }

  def toBits(n: BigInt): Seq[Boolean] = {
    for (shamt <- 0 until width) yield ((n >> shamt) & 1) == 1
  }

  for(_ <- 0 to 100000) {
    val value = next
    val expected = count(value)

    poke(counter.io.in.valid, true)
    poke(counter.io.out.ready, true)
    toBits(value).zipWithIndex.foreach { case (v, idx) => poke(counter.io.in.bits(idx), v) }
    expect(counter.io.out.bits, expected, s"value: $value, expect: $expected")
    expect(counter.io.out.valid, true)
    expect(counter.io.in.ready, true)
  }
}

class BitCounterMultiCycleTester(counter: BitCounter, width: Int) extends PeekPokeTester(counter) {
  val random = new Random(0)
  def next: BigInt = BigInt(width, random)
  def count(value: BigInt): Int = {
    (0 until width).foldLeft(0) {
      case (acc, shamt) =>
        if(((value >> shamt) & 1) == 1) acc + 1
        else acc
    }
  }

  def toBits(n: BigInt): Seq[Boolean] = {
    for (shamt <- 0 until width) yield ((n >> shamt) & 1) == 1
  }

  for(_ <- 0 to 10000) {
    val value    = next
    val expected = count(value)

    poke(counter.io.in.valid, true)
    poke(counter.io.out.ready, true)
    toBits(value).zipWithIndex.foreach{ case (v, idx) => poke(counter.io.in.bits(idx), v) }

    step(1)
    poke(counter.io.in.valid, false)

    while(peek(counter.io.out.valid) != BigInt(1)) { step(1) }

    expect(counter.io.out.bits, expected, s"value: $value, expect: $expected")
    expect(counter.io.out.valid, true)
    expect(counter.io.in.ready, true)
    step(1)
  }
}

class BitCounterTest extends ChiselFlatSpec {
  "BitCounter for 1bit" should "works correctly" in {
    Driver(() => new BitCounter(1, 0), "treadle") {
      c => new BitCounterTester(c, 1)
    } should be (true)
  }

  "BitCounter for 2bit" should "works correctly" in {
    Driver(() => new BitCounter(2, 0), "treadle") {
      c => new BitCounterTester(c, 2)
    } should be (true)
  }

  "BitCounter for 4bit" should "works correctly" in {
    Driver(() => new BitCounter(4, 0), "treadle") {
      c => new BitCounterTester(c, 2)
    } should be (true)
  }

  "BitCounter for 32bit" should "works correctly" in {
    Driver(() => new BitCounter(32, 0), "treadle") {
      c => new BitCounterTester(c, 32)
    } should be (true)
  }

  "BitCounter for 32bit multi cycle" should "works correctly" in {
    Driver(() => new BitCounter(32, 4), "treadle") {
      c => new BitCounterMultiCycleTester(c, 32)
    } should be (true)
  }

  "BitCounter for 32bit multi cycle 2steps per cycle" should "works correctly" in {
    Driver(() => new BitCounter(32, 2), "treadle") {
      c => new BitCounterMultiCycleTester(c, 32)
    } should be (true)
  }

}
