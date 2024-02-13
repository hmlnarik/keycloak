import { expect, test } from "@playwright/test";
import {
  getUserByUsername,
  getCredentials,
  deleteCredential,
} from "../admin-client";
import { login } from "../login";

const realm = "groups";
test.describe("Signing in", () => {
  // Tests for keycloak account console, section Signing in in Account security
  test("Should see only password", async ({ page }) => {
    await login(page, "jdoe", "jdoe", "groups");

    await page.getByTestId("accountSecurity").click();
    await expect(page.getByTestId("account-security/signing-in")).toBeVisible();
    page.getByTestId("account-security/signing-in").click();

    await expect(
      page
        .getByTestId("basic-authentication/credential-list")
        .getByRole("listitem"),
    ).toHaveCount(1);
    await expect(
      page
        .getByTestId("basic-authentication/credential-list")
        .getByRole("listitem"),
    ).toContainText("My password");
    await expect(page.getByTestId("basic-authentication/create")).toBeHidden();

    await expect(
      page.getByTestId("two-factor/credential-list").getByRole("listitem"),
    ).toHaveCount(1);
    await expect(
      page.getByTestId("two-factor/credential-list").getByRole("listitem"),
    ).toContainText("not set up");
    await expect(page.getByTestId("two-factor/create")).toBeVisible();

    await page.getByTestId("two-factor/create").click();
    await expect(page.locator("#kc-page-title")).toContainText(
      "Mobile Authenticator Setup",
    );
  });
});

test.describe("Signing in 2", () => {
  test("Password removal", async ({ page }) => {
    const jdoeUser = await getUserByUsername("jdoe", realm);

    await login(page, "jdoe", "jdoe", "groups");

    const credentials = await getCredentials(jdoeUser!.id!, realm);
    deleteCredential(jdoeUser!.id!, credentials![0].id!, realm);

    await page.getByTestId("accountSecurity").click();
    await expect(page.getByTestId("account-security/signing-in")).toBeVisible();
    page.getByTestId("account-security/signing-in").click();

    await expect(
      page
        .getByTestId("basic-authentication/credential-list")
        .getByRole("listitem"),
    ).toHaveCount(1);
    await expect(
      page
        .getByTestId("basic-authentication/credential-list")
        .getByRole("listitem"),
    ).toContainText("not set up");
    await expect(page.getByTestId("basic-authentication/create")).toBeVisible();

    await expect(
      page.getByTestId("two-factor/credential-list").getByRole("listitem"),
    ).toHaveCount(1);
    await expect(
      page.getByTestId("two-factor/credential-list").getByRole("listitem"),
    ).toContainText("not set up");
    await expect(page.getByTestId("two-factor/create")).toBeVisible();

    await page.getByTestId("basic-authentication/create").click();
    await expect(page.locator("#kc-page-title")).toContainText(
      "Update password",
    );
  });
});
