const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');

(async () => {
  const [url, outputFile] = process.argv.slice(2);
  const cleanUrl = url.trim();
  const cleanOutputFile = outputFile.trim();
  console.log(cleanUrl)
  console.log(cleanOutputFile)

  // ✅ Robust URL validation
  try {
    new URL(cleanUrl);
  } catch {
    console.error('❌ Invalid URL format. Please provide a fully qualified URL.');
    process.exit(1);
  }

  if (!cleanOutputFile) {
    console.error('❌ Please provide a filename as the second argument.');
    process.exit(1);
  }

  let browser;
  try {
    console.log("1")
    browser = await puppeteer.launch({ headless: true });
    console.log("2")
    const page = await browser.newPage();
    console.log("3")
    await page.goto(cleanUrl);
    console.log("4")
    const buttonIconSelector = "button > div > i:first-of-type"
    await page.waitForSelector(buttonIconSelector);
    console.log("5")
    const html = await page.content();
    console.log("6")
    const filePath = path.resolve(cleanOutputFile);
    console.log("resolved file path", filePath)
    fs.writeFileSync(filePath, html, 'utf8');
    console.log(`✅ HTML saved to ${filePath}`);
  } catch (err) {
    console.error('⚠️ Failed to load page:', err.message);
  } finally {
    await browser.close();
  }
})();