const fs = require('fs');
const path = require('path');
const { Resvg } = require('@resvg/resvg-js');

async function main() {
  const inputPath = path.resolve(__dirname, '../public/cost-estimation.svg');
  const outputPath = path.resolve(__dirname, '../public/cost-estimation.png');

  if (!fs.existsSync(inputPath)) {
    console.error(`Input SVG not found at: ${inputPath}`);
    process.exit(1);
  }

  try {
    const svgBuffer = fs.readFileSync(inputPath);
    const resvg = new Resvg(svgBuffer, {
      fitTo: { mode: 'width', value: 1800 },
      background: 'white'
    });
    const pngData = resvg.render().asPng();
    fs.writeFileSync(outputPath, pngData);
    console.log(`PNG generated: ${outputPath}`);
  } catch (err) {
    console.error('Failed to generate PNG from SVG:', err);
    process.exit(1);
  }
}

main();


