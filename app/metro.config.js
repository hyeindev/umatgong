// https://docs.expo.dev/guides/customizing-metro/
const { getDefaultConfig } = require('expo/metro-config');

const config = getDefaultConfig(__dirname);

// 웹 폰트(Pretendard woff2)를 에셋으로 번들한다. 기본값에는 otf·ttf만 있다.
config.resolver.assetExts.push('woff2');

module.exports = config;
