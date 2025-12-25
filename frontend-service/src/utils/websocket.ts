import type { NotificationDto } from '../types';

type MessageHandler = (notification: NotificationDto) => void;
type ErrorHandler = (error: Event) => void;
type CloseHandler = () => void;

export class NotificationWebSocket {
  private ws: WebSocket | null;
  private messageHandlers: Set<MessageHandler>;
  private errorHandlers: Set<ErrorHandler>;
  private closeHandlers: Set<CloseHandler>;
  private reconnectTimeout: number | null;
  private shouldReconnect: boolean;
  private reconnectDelay: number;
  private getToken: () => string | null;

  constructor(getToken: () => string | null) {
    this.getToken = getToken;
    this.ws = null;
    this.messageHandlers = new Set();
    this.errorHandlers = new Set();
    this.closeHandlers = new Set();
    this.reconnectTimeout = null;
    this.shouldReconnect = true;
    this.reconnectDelay = 3000; // 3 seconds
  }


  connect() {
    const token = this.getToken();
    if (!token) {
      console.warn('No auth token available, cannot connect to WebSocket');
      return;
    }

    const wsUrl = this.getWebSocketUrl();

    try {
      this.ws = new WebSocket(wsUrl);

      this.ws.onopen = () => {
        console.log('WebSocket connected');
        // Reset reconnect delay on successful connection
        this.reconnectDelay = 3000;
      };

      this.ws.onmessage = (event) => {
        try {
          const notification: NotificationDto = JSON.parse(event.data);
          this.messageHandlers.forEach(handler => handler(notification));
        } catch (error) {
          console.error('Failed to parse WebSocket message:', error);
        }
      };

      this.ws.onerror = (error) => {
        console.error('WebSocket error:', error);
        this.errorHandlers.forEach(handler => handler(error));
      };

      this.ws.onclose = () => {
        console.log('WebSocket closed');
        this.closeHandlers.forEach(handler => handler());

        // Attempt to reconnect
        if (this.shouldReconnect) {
          this.scheduleReconnect();
        }
      };
    } catch (error) {
      console.error('Failed to create WebSocket:', error);
    }
  }

  private getWebSocketUrl(): string {
    const baseUrl = import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8080';
    const token = this.getToken();

    // Convert HTTP(S) to WS(S)
    const wsProtocol = baseUrl.startsWith('https') ? 'wss' : 'ws';
    const urlWithoutProtocol = baseUrl.replace(/^https?:\/\//, '');

    // Include token in URL as Authorization header is not supported in browser WebSocket
    return `${wsProtocol}://${urlWithoutProtocol}/api/v0.0.1/notifications/ws?token=${token}`;
  }

  private scheduleReconnect() {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
    }

    console.log(`Reconnecting in ${this.reconnectDelay}ms...`);
    this.reconnectTimeout = window.setTimeout(() => {
      console.log('Attempting to reconnect...');
      this.connect();
      // Exponential backoff, max 30 seconds
      this.reconnectDelay = Math.min(this.reconnectDelay * 1.5, 30000);
    }, this.reconnectDelay);
  }

  disconnect() {
    this.shouldReconnect = false;

    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }

    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }

  onMessage(handler: MessageHandler) {
    this.messageHandlers.add(handler);
    return () => this.messageHandlers.delete(handler);
  }

  onError(handler: ErrorHandler) {
    this.errorHandlers.add(handler);
    return () => this.errorHandlers.delete(handler);
  }

  onClose(handler: CloseHandler) {
    this.closeHandlers.add(handler);
    return () => this.closeHandlers.delete(handler);
  }

  isConnected(): boolean {
    return this.ws !== null && this.ws.readyState === WebSocket.OPEN;
  }
}
