package com.hightemp.offline_tube.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hightemp.offline_tube.domain.model.VideoQuality

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun QualitySelector(
    selectedQuality: VideoQuality,
    onQualitySelected: (VideoQuality) -> Unit,
    modifier: Modifier = Modifier
) {
    val qualities = VideoQuality.entries

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Качество видео",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            qualities.forEachIndexed { index, quality ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = qualities.size
                    ),
                    onClick = { onQualitySelected(quality) },
                    selected = quality == selectedQuality,
                    label = { Text(quality.label) }
                )
            }
        }
    }
}
