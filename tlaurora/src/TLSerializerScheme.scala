package org.chipsalliance.tilelink.tlaurora

/** Here is some details about the encoding:
  * channel remap:
  *  A -> 0
  *  B -> 1
  *
  * opcode remap:
  *  Get            -> 00
  *  PutFullData    -> 10
  *  PutPartialData -> 11
  *  AccessAckData  -> 1x (x encode the denied bit)
  *  AccessAck      -> 0x (x encode the denied bit)
  *
  * param tie 0
  *
  *                        A       |     Get    PutFullData    PutPartialData
  * Channel [0]            0       |      0          0               0
  * Opcode  [1:0]      a_opcode    |      00         10              11
  * Corrupt [0]        a_corrupt   |      0          x               x
  * Size    [3:0]      a_size      |      x          x               x
  *
  * Source  [SW:0]     a_source    |      x          x               x
  * Address [AW:0]     a_address   |      x          x               x
  * Data    [DW:0]     a_data      |      nil        x               x
  * Mask    [DW/8:0]   a_mask      |      nil        nil             x
  *
  *                        D       |   AccessAck     AccessAckData
  * Channel [0]            1       |   1               1
  * Opcode  [0]        d_hasData   |   0               1
  * Denied  [0]        d_denied    |   x               x
  * Corrupt [0]        d_corrupt   |   x               x
  * Size    [3:0]      d_size      |   x               x
  *
  * Source  [SW:0]     d_source    |   x               x
  * Data    [DW:0]     d_data      |   nil             x
  *
  * Get:
  * Octet[0]                            -> metadata
  * Octet[1, SW)                        -> source
  * Octet[SW, SW+AW-1)                  -> address
  *
  * PutFullData:
  * Octet[0]                            -> metadata
  * Octet[1, SW)                        -> source
  * Octet[SW, SW+AW-1)                  -> address
  * Octet[SW+AW, SW+AW+DW-1)            -> data
  *
  * PutPartialData:
  * Octet[0]                            -> metadata
  * Octet[1, SW)                        -> source
  * Octet[SW, SW+AW-1)                  -> address
  * Octet[SW+AW, SW+AW+DW-1)            -> data
  * Octet[SW+AW+DW, SW+AW+DW+[DW/8])    -> mask
  *
  * AccessAck:
  * Octet[0]                            -> metadata
  * Octet[1, SW)                        -> source
  *
  * AccessAckData:
  * Octet[0]                            -> metadata
  * Octet[1, SW)                        -> source
  * Octet[SW, SW+DW-1)                  -> data
  */
case object TLUHNoAtomic extends TLSerializerScheme
case object TLUH extends TLSerializerScheme
case object TLC extends TLSerializerScheme

/** [[TLSerializerScheme]] support 3 encoding schemes:
  * 1. [[TLUHNoAtomic]]: TL-UH without atomic: lightweight but providing a huge bandwidth
  * 2. [[TLUH]]: Standard TL-UH
  * 3. [[TLC]]: TL-C
  *
  * Since there is [[TLUH]] will cover TLUL, and the bandwidth of TLUL is limited,
  * the scheme minimal support is [[TLUHNoAtomic]]
  */
sealed trait TLSerializerScheme
