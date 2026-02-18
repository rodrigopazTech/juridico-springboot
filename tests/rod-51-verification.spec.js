
const { test, expect } = require('@playwright/test');

test('ROD-51: Verify Lawyer Assignment Logic', async ({ page }) => {
    // 1. Login
    await page.goto('http://localhost:8080/login');
    await page.fill('input[name="username"]', 'director@juridico.com');
    await page.fill('input[name="password"]', '12345');
    await page.click('button[type="submit"]');

    // Verify login success
    await expect(page).toHaveURL('http://localhost:8080/dashboard');

    // 2. Navigate to Audiencias
    await page.click('a[href="/audiencias"]');
    await expect(page).toHaveURL('http://localhost:8080/audiencias');

    // 3. Open "Nueva Audiencia" Modal
    // The button has text "Nueva Audiencia"
    await page.getByRole('button', { name: 'Nueva Audiencia' }).click();

    // Wait for modal to be visible
    const modal = page.locator('#modal-audiencia');
    await expect(modal).toBeVisible();

    // 4. Check "Abogado que Comparece" Dropdown
    const abogadoSelect = page.locator('#input-abogado-audiencia');

    // Get all options
    const options = await abogadoSelect.locator('option').allTextContents();

    console.log('Abogados disponibles:', options);

    // Filter out the default "-- Seleccionar Abogado (Opcional) --"
    const validOptions = options.filter(opt => !opt.includes('-- Seleccionar'));

    // Verify there are lawyers available
    expect(validOptions.length).toBeGreaterThan(0);

    // Ideally, we should check against known data, but for now we verify the list is populated.
    // If we want to be stricter, we need to know existing lawyers in DB.
    // Assuming Director sees everyone, the list should be substantial.
});
