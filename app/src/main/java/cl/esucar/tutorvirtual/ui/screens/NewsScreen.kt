package cl.esucar.tutorvirtual.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.esucar.tutorvirtual.ui.theme.InstitutionalColors

@Composable
fun NewsScreen(
    onOpenRevista: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Noticias institucionales",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = InstitutionalColors.GreenDark
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Accede a las publicaciones oficiales de la Revista Carabineros.",
                color = InstitutionalColors.MutedText,
                fontSize = 14.sp
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = InstitutionalColors.NavySoft),
                border = BorderStroke(1.dp, InstitutionalColors.Navy.copy(alpha = 0.18f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = InstitutionalColors.Navy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Newspaper,
                                contentDescription = null,
                                tint = InstitutionalColors.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Text(
                        "Revista Carabineros",
                        color = InstitutionalColors.Navy,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Consulta noticias, reportajes y contenidos institucionales directamente desde el portal oficial.",
                        color = InstitutionalColors.Graphite,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Button(
                        onClick = onOpenRevista,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InstitutionalColors.Navy,
                            contentColor = InstitutionalColors.White
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Abrir Revista Carabineros", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = InstitutionalColors.White),
                border = BorderStroke(1.dp, InstitutionalColors.Border),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, null, tint = InstitutionalColors.Green)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Fuente oficial", fontWeight = FontWeight.SemiBold, color = InstitutionalColors.Graphite)
                        Text(
                            "Los contenidos se consultan en el sitio institucional para conservar la información actualizada.",
                            color = InstitutionalColors.MutedText,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
