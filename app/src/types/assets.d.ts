// 번들러가 폰트 파일을 에셋 참조로 바꿔 준다 (네이티브는 에셋 번호, 웹은 URL).
declare module '*.otf' {
  const source: number | string;
  export default source;
}

declare module '*.woff2' {
  const source: number | string;
  export default source;
}
