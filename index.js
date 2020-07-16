/**
 * @format
 */

import { AppRegistry } from 'react-native';
import App from './App';
import { name as appName } from './app.json';
import './shim';

const DHT = require('bittorrent-dht');

 const dht = new DHT();

// dht.listen(20000, () => {
//   console.log('now listening');
// });

// dht.on('peer', (peer, infoHash, from) => {
//   console.log(`found potential peer ${peer.host}:${peer.port} through ${from.address}:${from.port}`);
// });

// find peers for the given torrent info hash
// dht.lookup(parsed.infoHash);


AppRegistry.registerComponent(appName, () => App);
