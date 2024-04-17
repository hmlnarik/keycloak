import { test } from "@playwright/test";
import { VirtualAuthenticators, Authenticators } from "./chrome-utils";

test.describe("WebAuthn", () => {
  test("Check virtual autenticators work", async ({ page }) => {
    const vAuth = await VirtualAuthenticators.create(page);

    const addedVAuth = await vAuth.addVirtualAuthenticator({
      options: Authenticators.DEFAULT_USB,
    });

    await vAuth.clearCredentials(addedVAuth);
    await vAuth.removeVirtualAuthenticator(addedVAuth);
  });
});
