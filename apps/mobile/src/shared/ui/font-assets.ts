import ArchivoBlack from "../../../assets/fonts/Archivo-Black.ttf";
import ArchivoExpandedBlack from "../../../assets/fonts/ArchivoExpanded-Black.ttf";
import ManropeBold from "../../../assets/fonts/Manrope-Bold.ttf";
import ManropeExtraBold from "../../../assets/fonts/Manrope-ExtraBold.ttf";
import ManropeMedium from "../../../assets/fonts/Manrope-Medium.ttf";
import ManropeRegular from "../../../assets/fonts/Manrope-Regular.ttf";
import ManropeSemiBold from "../../../assets/fonts/Manrope-SemiBold.ttf";
import { fonts } from "./theme";

// Instâncias estáticas das fontes variáveis Archivo e Manrope (OFL, ver assets/fonts/OFL-*.txt):
// o React Native não aplica o eixo de largura, então a Archivo larga vem pronta em 125%.
export const fontAssets = {
  [fonts.display]: ArchivoExpandedBlack,
  [fonts.number]: ArchivoBlack,
  [fonts.regular]: ManropeRegular,
  [fonts.medium]: ManropeMedium,
  [fonts.semibold]: ManropeSemiBold,
  [fonts.bold]: ManropeBold,
  [fonts.extrabold]: ManropeExtraBold,
};
