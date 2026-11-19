const zlib = require('zlib');
const { Tray, Menu, nativeImage } = require('electron');

function buildProgrammaticTrayPng() {
  const width = 32;
  const height = 32;
  const raw = Buffer.alloc((width * 4 + 1) * height);

  for (let y = 0; y < height; y++) {
    const rowStart = y * (width * 4 + 1);
    raw[rowStart] = 0;
    for (let x = 0; x < width; x++) {
      const px = rowStart + 1 + x * 4;
      const inBorder = x >= 3 && x <= 28 && y >= 3 && y <= 28;
      const inBow = y >= 10 && y <= 22 && x >= 8 && x <= 23 && (x + y > 22);
      if (inBow) {
        raw[px] = 0x22;
        raw[px + 1] = 0xd3;
        raw[px + 2] = 0xee;
        raw[px + 3] = 0xff;
      } else if (inBorder) {
        raw[px] = 0x0b;
        raw[px + 1] = 0x15;
        raw[px + 2] = 0x28;
        raw[px + 3] = 0xff;
      } else {
        raw[px] = 0x3b;
        raw[px + 1] = 0x82;
        raw[px + 2] = 0xf6;
        raw[px + 3] = 0xff;
      }
    }
  }

  function crc32(buf) {
    let c = 0xffffffff;
    for (let i = 0; i < buf.length; i++) {
      c ^= buf[i];
      for (let k = 0; k < 8; k++) {
        c = (c >>> 1) ^ (c & 1 ? 0xedb88320 : 0);
      }
    }
    return (c ^ 0xffffffff) >>> 0;
  }

  function chunk(type, data) {
    const len = Buffer.alloc(4);
    len.writeUInt32BE(data.length, 0);
    const typeBuf = Buffer.from(type, 'ascii');
    const crcBuf = Buffer.alloc(4);
    crcBuf.writeUInt32BE(crc32(Buffer.concat([typeBuf, data])), 0);
    return Buffer.concat([len, typeBuf, data, crcBuf]);
  }

  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8;
  ihdr[9] = 6;
  const idat = zlib.deflateSync(raw);

  return Buffer.concat([
    Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
    chunk('IHDR', ihdr),
    chunk('IDAT', idat),
    chunk('IEND', Buffer.alloc(0))
  ]);
}

class TrayManager {
  constructor(onShow, onQuit) {
    this.onShow = onShow;
    this.onQuit = onQuit;
    this.tray = null;
    this.notifiedOnce = false;
  }

  init() {
    const icon = nativeImage.createFromBuffer(buildProgrammaticTrayPng());
    this.tray = new Tray(icon);
    this.tray.setToolTip('Ferizy Multi-Container Browser (Fase 4 • 24 Jam Siap)');

    const menu = Menu.buildFromTemplate([
      { label: 'Buka Jendela Ferizy Browser', click: () => this.onShow() },
      { type: 'separator' },
      { label: 'Mode 24 Jam (Sniffer 0ms + Notif Main Process)', enabled: false },
      { type: 'separator' },
      { label: 'Keluar Total', click: () => this.onQuit() }
    ]);
    this.tray.setContextMenu(menu);
    this.tray.on('double-click', () => this.onShow());
  }

  notifyHiddenToTray() {
    if (this.tray && !this.notifiedOnce && process.platform === 'win32') {
      this.notifiedOnce = true;
      this.tray.displayBalloon({
        title: 'Ferizy Multi-Container Berjalan di Latar',
        content: '5 container akun tetap aktif 24 jam. Klik ganda ikon tray untuk membuka kembali.'
      });
    }
  }

  destroy() {
    if (this.tray) {
      this.tray.destroy();
      this.tray = null;
    }
  }
}

module.exports = { TrayManager };
