package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextGray

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("about_screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        
        Spacer(modifier = Modifier.height(10.dp))

        // --- Header Section ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(BluePrimary.copy(alpha = 0.08f))
                    .border(2.dp, BluePrimary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("👨‍💻", fontSize = 44.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "About Developer",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextDark,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Fullstack Developer & Android App Developer",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextGray,
                textAlign = TextAlign.Center
            )
        }

        // --- Profile Card ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(16.dp), ambientColor = Color.LightGray),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Profil Developer",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
                
                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Nama", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold)
                    Text(text = "Labib Achmad", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Role", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Fullstack Developer & Android App Developer",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Deskripsi", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Saya adalah seorang Fullstack Developer dan Android App Developer yang berfokus pada pengembangan aplikasi modern, responsif, dan mudah digunakan.",
                        fontSize = 12.sp,
                        color = TextDark,
                        lineHeight = 18.sp
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Keahlian Utama", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold)
                    val skills = listOf(
                        "Flutter", "Android Development", "React.js", "Laravel", 
                        "Python", "Streamlit", "MySQL", "PostgreSQL", 
                        "REST API", "UI/UX Design"
                    )
                    skills.forEach { skill ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 1.dp)
                        ) {
                            Text(text = "•", fontSize = 12.sp, color = BluePrimary, fontWeight = FontWeight.Bold)
                            Text(text = skill, fontSize = 12.sp, color = TextDark, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // --- Tech Stack Card ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(16.dp), ambientColor = Color.LightGray),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🚀", fontSize = 18.sp)
                    Text(
                        text = "Tech Stack",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }

                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // Render badges in clean helper rows
                val techStack = listOf(
                    "Flutter", "Dart", "Laravel", "React", "Python", 
                    "Streamlit", "MySQL", "PostgreSQL", "Git", "GitHub"
                )
                
                // Group technical badges in list chunks for maximum compatibility
                val badgeChunks = techStack.chunked(3)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    badgeChunks.forEach { chunk ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            chunk.forEach { tech ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(30.dp))
                                        .background(BluePrimary.copy(alpha = 0.08f))
                                        .border(1.dp, BluePrimary.copy(alpha = 0.15f), RoundedCornerShape(30.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = tech,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BluePrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Featured Project Card ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(16.dp), ambientColor = Color.LightGray),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📱", fontSize = 18.sp)
                    Text(
                        text = "Featured Project",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }

                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BluePrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = BluePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Smart Student Manager",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }
                    Text(
                        text = "Aplikasi produktivitas mahasiswa yang membantu mengelola tugas, keuangan, nilai akademik, dan prediksi IPK dalam satu platform.",
                        fontSize = 12.sp,
                        color = TextGray,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // --- Contact Card ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(16.dp), ambientColor = Color.LightGray),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📬", fontSize = 18.sp)
                    Text(
                        text = "Contact",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }

                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ContactRowItem(
                        icon = Icons.Default.Link,
                        label = "GitHub",
                        value = "https://github.com/lamnaf"
                    )
                    ContactRowItem(
                        icon = Icons.Default.Email,
                        label = "Email",
                        value = "lamnaf.chestplayer@gmail.com"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- Light Mode & Portfolio Footer ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Text(
                text = "Made with ❤️ by Labib Achmad",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Fullstack Developer & Android App Developer",
                fontSize = 10.sp,
                color = TextGray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ContactRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SlateBackground)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(BluePrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BluePrimary,
                modifier = Modifier.size(16.dp)
            )
        }
        Column {
            Text(text = label, fontSize = 9.sp, color = TextGray, fontWeight = FontWeight.Bold)
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }
    }
}
