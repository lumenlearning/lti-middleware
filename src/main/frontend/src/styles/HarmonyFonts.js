//this was copied over from Valkyrie following the Harmony Global Style Guide
import { createGlobalStyle } from "styled-components";
import Lora from "./fonts/lora_regular.woff2";
import PublicSans400 from "./fonts/public_sans_400.woff2";
import PublicSans600 from "./fonts/public_sans_600.woff2";
import PublicSans700 from "./fonts/public_sans_700.woff2";
import { EColor } from "./HarmonyColors";

const HarmonyFonts = createGlobalStyle`
@font-face {
    font-family: 'Public Sans';
    font-style: normal;
    font-weight: 400;
    src: url(${PublicSans400}) format('woff2');
}
@font-face {
    font-family: 'Public Sans';
    font-style: normal;
    font-weight: 600;
    src: url(${PublicSans600}) format('woff2');
}
@font-face {
    font-family: 'Public Sans';
    font-style: normal;
    font-weight: 700;
    src: url(${PublicSans700}) format('woff2');
}
@font-face {
    font-family: 'Lora';
    font-style: normal;
    font-weight: 400;
    src: url(${Lora}) format('woff2');
}
body {
    background-color: ${EColor.light_gray};
    color: ${EColor.primary_text};
    font-family: 'Public Sans', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen-Sans, Ubuntu, Cantarell, "Helvetica Neue", sans-serif;
    font-weight: 400;
    font-size: 16px;
    line-height: 1.4;
    margin: 0;
    padding: 0;

    b, strong{
        font-weight: 600;
    }
}
h1,h2,h3,h4 {
    margin: 0;
}
h1 {
    font-size: 48px;
    font-weight: 600;
    letter-spacing: -0.02em;
    line-height: 1.1;
 }
 h2 {
    font-size: 36px;
    font-weight: 600;
    letter-spacing: -0.02em;
    line-height: 1.2;
 }
 h3 {
     font-size: 26px;
     font-weight: 600;
     line-height: 1.2;
 }
 h4 {
    font-size: 20px;
    font-weight: 600;
    line-height: 1.3;
 }
 figcaption, caption, .caption {
    font-size: 14px;
    font-weight: 700;
    line-height: 1.2;
 }
 blockquote, .pullQuote {
    font-family: 'Lora',  -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen-Sans, Ubuntu, Cantarell, "Helvetica Neue", sans-serif;
    font-size: 16px;
    font-weight: 400;
    line-height: 1.4;
    @media (min-width: 768px){
        font-size: 20px;
    }
 }
 input[type="checkbox"]{
    accent-color: ${EColor.white};
    border-radius: 4px;
    &:checked{
        accent-color: ${EColor.indigo_500};
    }
 }

 ol, ul {
    font-size: 1rem;
 }
`;

export { HarmonyFonts };
