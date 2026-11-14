package cl.esucar.tutorvirtual.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.esucar.tutorvirtual.R
import cl.esucar.tutorvirtual.ui.theme.InstitutionalColors

@Composable
fun InstitutionalBrand() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(R.drawable.institutional_header_logo),
            contentDescription = "Emblema Carabineros de Chile ESUCAR",
            modifier = Modifier
                .height(36.dp)
                .width(32.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                "Tutor Virtual ESUCAR",
                color = InstitutionalColors.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Escuela de Suboficiales de Carabineros",
                color = InstitutionalColors.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
