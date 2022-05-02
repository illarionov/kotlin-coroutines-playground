package coroutines

import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class FlowTest {


    @Test
    fun `StateFlow эммитит дефолтное значение`() {
        val stateFlow = MutableStateFlow<UIState>(UIState.Success)

        runTest {
            stateFlow.test {
                expectItem() shouldBe UIState.Success
            }
        }
    }


    sealed class UIState {
        object Success : UIState()
        object Error : UIState()
    }
}