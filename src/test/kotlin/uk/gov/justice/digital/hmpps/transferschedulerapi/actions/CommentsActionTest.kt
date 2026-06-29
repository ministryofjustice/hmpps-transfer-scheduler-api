package uk.gov.justice.digital.hmpps.transferschedulerapi.actions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.transferschedulerapi.model.action.CommentsAction

class CommentsActionTest {
  @ParameterizedTest
  @MethodSource("changeResults")
  fun `correctly identifies comments have changed`(originalComment: String?, newComment: String?, hasChanged: Boolean) {
    val action = object : CommentsAction {
      override val comments: String? = newComment
    }
    assertThat(action changes originalComment).isEqualTo(hasChanged)
  }

  companion object {
    private val LOREM_IPSUM = """
      |Lorem ipsum dolor sit amet, consectetur adipiscing elit. 
      |Etiam elementum velit efficitur, posuere sem elementum, rhoncus arcu. 
      |Nam vitae massa ut tortor tincidunt tincidunt ultricies vel eros. 
      |Nam volutpat euismod commodo. Donec aliquet mi ut mauris fusce.
    """.trimMargin()

    @JvmStatic
    fun changeResults() = listOf(
      Arguments.of(null, null, false),
      Arguments.of("", "", false),
      Arguments.of(LOREM_IPSUM, LOREM_IPSUM, false),
      // text truncated by sync
      Arguments.of(LOREM_IPSUM, LOREM_IPSUM.substring(0, 200) + "... see DPS for full text", false),
      // text intentionally shortened by a user
      Arguments.of(LOREM_IPSUM, LOREM_IPSUM.substring(0, 200), true),
      Arguments.of(LOREM_IPSUM, "Any other text", true),
      Arguments.of(LOREM_IPSUM, null, true),
      Arguments.of(null, LOREM_IPSUM, true),
      Arguments.of("", null, true),
      Arguments.of(null, "", true),
    )
  }
}
