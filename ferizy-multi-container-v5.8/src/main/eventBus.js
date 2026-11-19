const { EventEmitter } = require('events');

// Main Process Event Bus (<1ms) untuk sinyal bangun lintas tab/akun tanpa polling
class MainEventBus extends EventEmitter {
  constructor() {
    super();
    this.setMaxListeners(64);
    this.lastEvent = null;
  }

  broadcast(channel, payload) {
    const packet = {
      channel,
      payload,
      emittedAt: Date.now()
    };
    this.lastEvent = packet;
    this.emit(channel, packet);
    this.emit('*', packet);
    return packet;
  }
}

module.exports = { MainEventBus };
