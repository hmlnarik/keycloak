import { CDPSession, Page } from "@playwright/test";
import { Protocol } from "playwright-core/types/protocol";

export const Authenticators: {
  [key: string]: Protocol.WebAuthn.VirtualAuthenticatorOptions;
} = {
  DEFAULT: {
    protocol: "ctap2",
    transport: "usb",
  },
  DEFAULT_USB: {
    protocol: "ctap2",
    transport: "usb",
  },
  DEFAULT_BLE: {
    protocol: "ctap2",
    transport: "ble",
  },
  DEFAULT_NFC: {
    protocol: "ctap2",
    transport: "nfc",
  },
  DEFAULT_INTERNAL: {
    protocol: "ctap2",
    transport: "internal",
  },
  DEFAULT_RESIDENT_KEY: {
    protocol: "ctap2",
    transport: "usb",
    hasResidentKey: true,
    hasUserVerification: true,
    isUserVerified: true,
  },
};

export class VirtualAuthenticators {
  // CDP session
  #cdpSession: CDPSession;

  // static instance creator
  static async create(page: Page) {
    const cdpSession = await page.context().newCDPSession(page);
    const res = new VirtualAuthenticators(cdpSession);
    await res.enableWebAuthn();
    return res;
  }

  constructor(cdpSession: CDPSession) {
    this.#cdpSession = cdpSession;
  }

  enableWebAuthn = async () => {
    return this.#cdpSession.send("WebAuthn.enable");
  };

  addCredential = async (
    options: Protocol.CommandParameters["WebAuthn.addCredential"],
  ) => {
    return this.#cdpSession.send("WebAuthn.addCredential", options);
  };

  addVirtualAuthenticator = async (
    options: Protocol.CommandParameters["WebAuthn.addVirtualAuthenticator"],
  ) => {
    return this.#cdpSession.send("WebAuthn.addVirtualAuthenticator", options);
  };

  clearCredentials = async (
    options: Protocol.CommandParameters["WebAuthn.clearCredentials"],
  ) => {
    this.#cdpSession.send("WebAuthn.clearCredentials", options);
  };

  removeCredential = async (
    options: Protocol.CommandParameters["WebAuthn.removeCredential"],
  ) => {
    this.#cdpSession.send("WebAuthn.removeCredential", options);
  };

  removeVirtualAuthenticator = async (
    options: Protocol.CommandParameters["WebAuthn.removeVirtualAuthenticator"],
  ) => {
    this.#cdpSession.send("WebAuthn.removeVirtualAuthenticator", options);
  };
}
