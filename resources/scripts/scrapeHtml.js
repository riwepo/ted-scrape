const puppeteer = require('puppeteer');

(async () => {
  const [url] = process.argv.slice(2);
  const cleanUrl = url.trim();

  // ✅ Robust URL validation
  try {
    new URL(cleanUrl);
  } catch {
    console.error('❌ Invalid URL format. Please provide a fully qualified URL.');
    process.exit(1);
  }

  let browser;
  try {
    browser = await puppeteer.launch({ headless: true });
    const page = await browser.newPage();
    await page.goto(cleanUrl);
    const buttonIconSelector = "button > div > i:first-of-type"
    await page.waitForSelector(buttonIconSelector);
    const html = await page.content();
    console.log(html)
  } catch (err) {
    console.error('⚠️ Failed to load page:', err.message);
    process.exit(1)
  } finally {
    await browser.close();
  }
})();