#!/usr/bin/env python3
"""Tiny Modbus TCP client for the ESP32_MODBUS_LED sketch.

Uses only the Python standard library (sockets + struct), so there's nothing
to pip-install. Speaks just enough of Modbus TCP to read holding registers
(FC3) and write one or many holding registers (FC6 / FC16).

Register map matches the sketch:
  0..26  : 9 LEDs * (R, G, B), each 0..255
  27     : master brightness 0..255
  28     : master enable 0/1

Examples (replace 192.168.1.50 with your ESP32 IP):

  # turn the strip on at brightness 64
  python3 modbus_client.py 192.168.1.50 enable 1
  python3 modbus_client.py 192.168.1.50 brightness 64

  # set LED 0 to red, LED 1 to green, LED 2 to blue
  python3 modbus_client.py 192.168.1.50 led 0 255 0 0
  python3 modbus_client.py 192.168.1.50 led 1 0 255 0
  python3 modbus_client.py 192.168.1.50 led 2 0 0 255

  # set every LED to the same color in one round-trip
  python3 modbus_client.py 192.168.1.50 fill 128 0 200

  # read everything back
  python3 modbus_client.py 192.168.1.50 read
"""

import socket
import struct
import sys

NUM_LEDS    = 9
HR_RGB_BASE   = 0
HR_BRIGHTNESS = HR_RGB_BASE + NUM_LEDS * 3   # 27
HR_ENABLE     = HR_BRIGHTNESS + 1            # 28
HR_TOTAL      = HR_ENABLE + 1                # 29

UNIT_ID = 1
PORT    = 502
TIMEOUT = 3.0


class ModbusError(Exception):
    pass


def _txn(sock, tid, pdu):
    mbap = struct.pack(">HHHB", tid, 0, len(pdu) + 1, UNIT_ID)
    sock.sendall(mbap + pdu)

    head = _recv_exact(sock, 7)
    rtid, pid, length, uid = struct.unpack(">HHHB", head)
    if pid != 0:
        raise ModbusError(f"bad protocol id {pid}")
    body = _recv_exact(sock, length - 1)
    fc = body[0]
    if fc & 0x80:
        raise ModbusError(f"modbus exception fc={fc & 0x7F:#x} code={body[1]:#x}")
    return body


def _recv_exact(sock, n):
    buf = b""
    while len(buf) < n:
        chunk = sock.recv(n - len(buf))
        if not chunk:
            raise ModbusError("connection closed mid-frame")
        buf += chunk
    return buf


def read_holding(host, addr, qty):
    with socket.create_connection((host, PORT), timeout=TIMEOUT) as s:
        body = _txn(s, 1, struct.pack(">BHH", 0x03, addr, qty))
        bc = body[1]
        if bc != 2 * qty:
            raise ModbusError(f"unexpected byte count {bc}")
        return list(struct.unpack(">" + "H" * qty, body[2:2 + bc]))


def write_single(host, addr, value):
    with socket.create_connection((host, PORT), timeout=TIMEOUT) as s:
        _txn(s, 1, struct.pack(">BHH", 0x06, addr, value & 0xFFFF))


def write_multiple(host, addr, values):
    payload = struct.pack(">BHHB", 0x10, addr, len(values), 2 * len(values))
    payload += struct.pack(">" + "H" * len(values), *(v & 0xFFFF for v in values))
    with socket.create_connection((host, PORT), timeout=TIMEOUT) as s:
        _txn(s, 1, payload)


def cmd_enable(host, args):
    write_single(host, HR_ENABLE, int(args[0]))


def cmd_brightness(host, args):
    write_single(host, HR_BRIGHTNESS, int(args[0]))


def cmd_led(host, args):
    idx, r, g, b = (int(x) for x in args[:4])
    if not 0 <= idx < NUM_LEDS:
        raise SystemExit(f"led index must be 0..{NUM_LEDS - 1}")
    write_multiple(host, HR_RGB_BASE + 3 * idx, [r, g, b])


def cmd_fill(host, args):
    r, g, b = (int(x) for x in args[:3])
    write_multiple(host, HR_RGB_BASE, [r, g, b] * NUM_LEDS)


def cmd_read(host, _args):
    regs = read_holding(host, 0, HR_TOTAL)
    for i in range(NUM_LEDS):
        r, g, b = regs[3 * i:3 * i + 3]
        print(f"  LED {i}: R={r:>3} G={g:>3} B={b:>3}")
    print(f"  brightness = {regs[HR_BRIGHTNESS]}")
    print(f"  enable     = {regs[HR_ENABLE]}")


COMMANDS = {
    "enable":     (cmd_enable,     "<0|1>"),
    "brightness": (cmd_brightness, "<0..255>"),
    "led":        (cmd_led,        "<idx> <r> <g> <b>"),
    "fill":       (cmd_fill,       "<r> <g> <b>"),
    "read":       (cmd_read,       ""),
}


def main(argv):
    if len(argv) < 3 or argv[2] not in COMMANDS:
        print("usage: modbus_client.py <host> <cmd> [args...]")
        for name, (_fn, usage) in COMMANDS.items():
            print(f"  {name} {usage}")
        return 1
    host = argv[1]
    fn, _ = COMMANDS[argv[2]]
    fn(host, argv[3:])
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
