import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { stompSockJsUrl } from './api'

export function createSockJsStompClient(connectHeaders: Record<string, string> = {}): Client {
  return new Client({
    webSocketFactory: () => new SockJS(stompSockJsUrl()) as unknown as WebSocket,
    reconnectDelay: 5000,
    heartbeatIncoming: 15000,
    heartbeatOutgoing: 15000,
    connectHeaders,
  })
}
