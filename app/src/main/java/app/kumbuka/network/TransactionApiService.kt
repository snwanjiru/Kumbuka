package app.kumbuka.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

// ─────────────────────────────────────────────────────────────────────────────
// DASHBOARD MODELS
// ─────────────────────────────────────────────────────────────────────────────

data class DashboardSummaryResponse(
    val totalLent: Double,
    val totalBorrowed: Double,
    val amountOwedToMe: Double,
    val amountIOwe: Double,
    val activeLoansLent: Int,
    val activeLoansBorrowed: Int,
    val overdueLoans: Int,
    val debtAging: Map<String, Double>? = null,
    val monthlyTrend: List<MonthlyTrend>? = null
)

data class MonthlyTrend(
    val month: String,
    val lent: Double,
    val borrowed: Double
)

// ─────────────────────────────────────────────────────────────────────────────
// LOAN MODELS (LENT)
// ─────────────────────────────────────────────────────────────────────────────

data class LoanLentRequest(
    val personName: String,
    val phoneNumber: String,
    @SerializedName("loanAmount")
    val amountLent: Double,
    val dateLent: String, // yyyy-MM-dd
    val dueDate: String?, // yyyy-MM-dd
    val notes: String?
)

data class LoanLentResponse(
    val id: Long,
    val personName: String,
    val phoneNumber: String,
    @SerializedName("loanAmount")
    val amountLent: Double,
    val dateLent: String,
    val dueDate: String?,
    val notes: String?,
    val amountPaid: Double,
    val balance: Double,
    val status: String
)

// ─────────────────────────────────────────────────────────────────────────────
// LOAN MODELS (BORROWED)
// ─────────────────────────────────────────────────────────────────────────────

data class LoanBorrowedRequest(
    val personName: String,
    val phoneNumber: String,
    @SerializedName("loanAmount")
    val amountBorrowed: Double,
    val dateBorrowed: String, // yyyy-MM-dd
    val dueDate: String?, // yyyy-MM-dd
    val notes: String?
)

data class LoanBorrowedResponse(
    val id: Long,
    val personName: String,
    val phoneNumber: String,
    @SerializedName("loanAmount")
    val amountBorrowed: Double,
    val dateBorrowed: String,
    val dueDate: String?,
    val notes: String?,
    val amountPaid: Double,
    val balance: Double,
    val status: String
)

// ─────────────────────────────────────────────────────────────────────────────
// PAYMENT MODELS
// ─────────────────────────────────────────────────────────────────────────────

data class PaymentRequest(
    val amount: Double
)

// ─────────────────────────────────────────────────────────────────────────────
// TransactionApiService — Retrofit interface for loans and dashboard
// ─────────────────────────────────────────────────────────────────────────────

interface TransactionApiService {

    // ── Dashboard ────────────────────────────────────────────────────────────
    @GET("api/dashboard/summary")
    suspend fun getDashboardSummary(): Response<DashboardSummaryResponse>

    // ── Loans Lent ───────────────────────────────────────────────────────────
    @POST("api/loans-lent")
    suspend fun createLoanLent(@Body request: LoanLentRequest): Response<LoanLentResponse>

    @GET("api/loans-lent")
    suspend fun getAllLoansLent(): Response<List<LoanLentResponse>>

    @GET("api/loans-lent/{id}")
    suspend fun getLoanLentById(@Path("id") id: Long): Response<LoanLentResponse>

    @PUT("api/loans-lent/{id}")
    suspend fun updateLoanLent(@Path("id") id: Long, @Body request: LoanLentRequest): Response<LoanLentResponse>

    @DELETE("api/loans-lent/{id}")
    suspend fun deleteLoanLent(@Path("id") id: Long): Response<Unit>

    @POST("api/loans-lent/{id}/payment")
    suspend fun recordLentPayment(@Path("id") id: Long, @Body request: PaymentRequest): Response<LoanLentResponse>

    // ── Loans Borrowed ───────────────────────────────────────────────────────
    @POST("api/loans-borrowed")
    suspend fun createLoanBorrowed(@Body request: LoanBorrowedRequest): Response<LoanBorrowedResponse>

    @GET("api/loans-borrowed")
    suspend fun getAllLoansBorrowed(): Response<List<LoanBorrowedResponse>>

    @GET("api/loans-borrowed/{id}")
    suspend fun getLoanBorrowedById(@Path("id") id: Long): Response<LoanBorrowedResponse>

    @PUT("api/loans-borrowed/{id}")
    suspend fun updateLoanBorrowed(@Path("id") id: Long, @Body request: LoanBorrowedRequest): Response<LoanBorrowedResponse>

    @DELETE("api/loans-borrowed/{id}")
    suspend fun deleteLoanBorrowed(@Path("id") id: Long): Response<Unit>

    @POST("api/loans-borrowed/{id}/payment")
    suspend fun recordBorrowedPayment(@Path("id") id: Long, @Body request: PaymentRequest): Response<LoanBorrowedResponse>
}
