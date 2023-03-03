import styled from "styled-components";
import { EColor } from "../styles/HarmonyColors";
import { HarmonyFonts } from "../styles/HarmonyFonts.js";
import ErrorIllustration from "../svg/ErrorIllustration.svg";

const ErrorMessage = () => {
  return (
    <>
      <HarmonyFonts />
      <Container>
        <ErrorMessageWrapper>
          <h3>Something's not working. Please try again later. </h3>
          <p>
            If the issue persists, contact{" "}
            <a
              href="http://support.lumenlearning.com/"
              rel="noreferrer"
              target="_blank"
            >
              Lumen Support
            </a>
            .
          </p>
          <ErrorIllustration />
        </ErrorMessageWrapper>
        <ErrorIllustrationAttribution>
          <a href="https://storyset.com/people">
            People Illustrations by Storyset
          </a>
        </ErrorIllustrationAttribution>
      </Container>
    </>
  );
};

const Container = styled.div`
  background-color: ${EColor.light_gray};
  padding-top: 40px;
  padding-right: 20px;
  padding-left: 20px;
`;

const ErrorMessageWrapper = styled.div`
  padding: 40px;
  background-color: ${EColor.white};
  border-radius: 10px;
  text-align: center;
  font-family: "Public Sans";

  svg {
    max-width: 100%;
    max-height: 378px;
  }

  a {
    color: ${EColor.indigo_500};
    font-weight: 700;
  }

  p {
    font-size: 16px;
    line-height: 1.6;
  }
`;

const ErrorIllustrationAttribution = styled.div`
  margin-top: 20px;
  text-align: right;

  a {
    color: ${EColor.secondary_text};
  }
`;

export { ErrorMessage };
