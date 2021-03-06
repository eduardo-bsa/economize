package com.eduardo.economise

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.anychart.charts.Pie
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.w3c.dom.Text
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class GraficoActivity : AppCompatActivity() {

    //UI
    lateinit var tvInicio: TextView
    lateinit var tvLimites: TextView
    lateinit var tvCategorias: TextView
    lateinit var tvSair: TextView
    lateinit var anyChartView: AnyChartView
    lateinit var spMes: Spinner
    lateinit var tvNull: TextView
    lateinit var tvGasto: TextView
    lateinit var tvSaldo: TextView
    lateinit var tvReceita: TextView
    lateinit var tvEcon: TextView
    lateinit var tvMaxValor: TextView
    lateinit var tvMinReceita: TextView
    lateinit var ivUm: ImageView
    lateinit var ivDois: ImageView
    lateinit var ivTres: ImageView
    lateinit var ivQuatro: ImageView
    lateinit var ivCinco: ImageView

    //Variáveis globais
    lateinit var categoria: MutableList<String>
    lateinit var valorGrafico: MutableList<BigDecimal>
    lateinit var dataEntries: MutableList<ValueDataEntry>
    lateinit var lancamentoList: MutableList<Lancamento>
    lateinit var mesList: MutableList<String>
    lateinit var valList: MutableList<Meta>
    var optionSpinner = ""
    var progressBar: AlertDialog? = null

    //BD
    var firebaseUser: FirebaseUser? = null
    var firebaseAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grafico)

        initialise()

        setupPieChart()

        getMetas()
    }

    private fun initialise() {
        progressBar = progressBar()

        tvInicio = findViewById(R.id.tvInicio)
        tvLimites = findViewById(R.id.tvLimites)
        tvCategorias = findViewById(R.id.tvCategorias)
        tvSair = findViewById(R.id.tvSair)
        tvEcon = findViewById(R.id.tvEcon)
        tvMaxValor = findViewById(R.id.tvMaxValor)
        tvMinReceita = findViewById(R.id.tvMinReceita)
        tvGasto = findViewById(R.id.tvGasto)
        tvReceita = findViewById(R.id.tvReceita)
        tvSaldo = findViewById(R.id.tvSaldo)
        ivUm = findViewById(R.id.ivUm)
        ivDois = findViewById(R.id.ivDois)
        ivTres = findViewById(R.id.ivTres)
        ivQuatro = findViewById(R.id.ivQuatro)
        ivCinco = findViewById(R.id.ivCinco)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseUser = firebaseAuth!!.getCurrentUser()

        ivUm.setOnClickListener {
            val intent = Intent(this@GraficoActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

            startActivity(intent)
        }

        ivTres.setOnClickListener {
            val intent = Intent(this@GraficoActivity, LimitesActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

            startActivity(intent)
        }

        ivQuatro.setOnClickListener {
            val intent = Intent(this@GraficoActivity, CategoriasActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

            startActivity(intent)
        }

        ivCinco.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            val intent = Intent(this@GraficoActivity, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            finish()
            startActivity(intent)
        }
    }

    private fun setupPieChart() {
        lancamentoList = mutableListOf()

        val queryLancamento = FirebaseDatabase.getInstance().getReference("lancamento")
            .orderByChild("usuario")
            .equalTo(firebaseUser?.getEmail().toString())

        queryLancamento.addListenerForSingleValueEvent(lancamentoEventListener)
    }

    var lancamentoEventListener: ValueEventListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            lancamentoList.clear()

            for (snapshot in dataSnapshot.children) {
                val lanc = snapshot.getValue(Lancamento::class.java)
                lancamentoList.add(lanc!!)
            }

            val option = ArrayList<String>()
            spMes = findViewById(R.id.spMes)
            mesList = mutableListOf()

            for (i in lancamentoList.indices) {
                if (!mesList.contains(lancamentoList[i].data.substring(3,6) + lancamentoList[i].data.substring(6).trim())) {
                    mesList.add(lancamentoList[i].data.substring(3,6) + lancamentoList[i].data.substring(6).trim())
                }
            }

            val sort = mesList.sortedBy { it.substring(3,7)+it.substring(0,2).toInt() }
            sort.reversed()

            sort.forEach { t: String ->
                option.add(t.trim())
            }

            spMes.adapter = ArrayAdapter<String>(this@GraficoActivity, R.layout.spinner_mes, option)

            val currentDate: String =
                SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(Date())

            for (i in lancamentoList.indices) {
                if (lancamentoList[i].data.substring(3,6) + lancamentoList[i].data.substring(6).trim() == currentDate) {
                    spMes.setSelection(sort.indexOf(currentDate))
                }
            }

            val pie: Pie = AnyChart.pie()

            anyChartView = findViewById(R.id.any_chart_view)
            tvNull = findViewById(R.id.tvNull)

            spMes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(p0: AdapterView<*>?) { }

                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    progressBar = progressBar()

                    var indice = 0
                    var despesa = 0

                    categoria = mutableListOf()
                    valorGrafico = mutableListOf()
                    dataEntries = mutableListOf()

                    categoria.clear()
                    valorGrafico.clear()
                    dataEntries.clear()

                    for (i in lancamentoList.indices) {
                        if (!categoria.contains(lancamentoList[i].categoria)
                            && lancamentoList[i].valor.substring(0,1) == "-"
                            && (lancamentoList[i].data.substring(3,6) + lancamentoList[i].data.substring(6).trim()) == option.get(p2)) {
                            categoria.add(lancamentoList[i].categoria)

                            var saldoDespesa = 0.00

                            for (z in lancamentoList.indices) {
                                if (lancamentoList[z].categoria == lancamentoList[i].categoria
                                    && lancamentoList[z].valor.substring(0,1) == "-"
                                    && (lancamentoList[z].data.substring(3,6) + lancamentoList[z].data.substring(6).trim()) == option.get(p2)) {
                                    val valorDespesa = lancamentoList[z].valor.substring(5).replace(",",".").toFloat()

                                    saldoDespesa += valorDespesa
                                    despesa ++
                                }
                            }
                            valorGrafico.add(BigDecimal(saldoDespesa).setScale(2, RoundingMode.HALF_EVEN))

                            dataEntries.add(ValueDataEntry(categoria[indice], valorGrafico[indice]))

                            indice++

                            tvGasto.text = "R$ " + BigDecimal(saldoDespesa).setScale(2, RoundingMode.HALF_EVEN).toString().replace(".",",")
                        }
                    }

                    var saldo = 0.00
                    var receita = 0.00

                    for (x in lancamentoList.indices) {
                        if (lancamentoList[x].valor.substring(0,1) == "-"
                            && (lancamentoList[x].data.substring(3,6) + lancamentoList[x].data.substring(6).trim()) == option.get(p2)) {
                            val valor = lancamentoList[x].valor.substring(5).replace(",",".").toFloat() * -1

                            saldo += valor
                        } else if ((lancamentoList[x].data.substring(3,6) + lancamentoList[x].data.substring(6).trim()) == option.get(p2)) {
                            val valor = lancamentoList[x].valor.substring(3).replace(",",".").toFloat()

                            saldo += valor
                            receita += valor
                        }
                    }

                    tvReceita.text = "R$ " + BigDecimal(receita).setScale(2, RoundingMode.HALF_EVEN).toString().replace(".",",")

                    if (BigDecimal(saldo).setScale(2, RoundingMode.HALF_EVEN) < 0.toBigDecimal()) {
                        saldo *= -1

                        tvSaldo.text = "- R$ " + BigDecimal(saldo).setScale(2, RoundingMode.HALF_EVEN).toString().replace(".",",")
                    } else {
                        tvSaldo.text = "R$ " + BigDecimal(saldo).setScale(2, RoundingMode.HALF_EVEN).toString().replace(".",",")
                    }

                    if (despesa == 0) {
                        tvGasto.text = ""
                    }

                    if (receita == 0.00) {
                        tvReceita.text = ""
                    }

                    if (dataEntries.isEmpty()) {
                        dataEntries.add(ValueDataEntry("", 0))

                        anyChartView.setVisibility(View.INVISIBLE)

                        tvNull.text = "Não existem despesas no mês selecionado"
                    } else {
                        pie.data(dataEntries as List<DataEntry>?)

                        anyChartView.setVisibility(View.VISIBLE)

                        tvNull.text = ""
                    }

                    optionSpinner = option.get(p2)

                    valList = mutableListOf()

                    val queryMetas = FirebaseDatabase.getInstance().getReference("meta")
                        .orderByChild("usuario")
                        .equalTo(firebaseUser?.getEmail().toString())

                    queryMetas.addListenerForSingleValueEvent(metasEventListener)

                    if (tvSaldo.text.toString().isNotEmpty() && tvEcon.text.toString().isNotEmpty() && tvEcon.text.toString() != "R$ 0,00") {
                        if (tvSaldo.text.toString().replace("R$ ", "").replace(",", ".").replace("-", "").toFloat() >
                            tvEcon.text.toString().replace("R$ ", "").replace(",", ".").replace("-", "").toFloat()) {
                            tvSaldo.setTextColor(Color.parseColor("#00A351"))
                        } else if (tvSaldo.text.toString().replace("R$ ", "").replace(",", ".").replace("-", "").toFloat() <
                            tvEcon.text.toString().replace("R$ ", "").replace(",", ".").replace("-", "").toFloat()) {
                            tvSaldo.setTextColor(Color.parseColor("#B84A43"))
                        }
                    }

                    if (tvGasto.text.toString().isNotEmpty() && tvMaxValor.text.toString().isNotEmpty() && tvMaxValor.text.toString() != "R$ 0,00") {
                        if (tvGasto.text.toString().replace("R$ ", "").replace(",", ".").replace("-", "").toFloat() >
                            tvMaxValor.text.toString().replace("R$ ", "").replace(",", ".").replace("-", "").toFloat()) {
                            tvGasto.setTextColor(Color.parseColor("#B84A43"))
                        } else if (tvGasto.text.toString().replace("R$ ", "").replace(",", ".").replace("-", "").toFloat() <
                            tvMaxValor.text.toString().replace("R$ ", "").replace(",", ".").replace("-", "").toFloat()) {
                            tvGasto.setTextColor(Color.parseColor("#00A351"))
                        }
                    }

                    if (tvReceita.text.toString().isNotEmpty() && tvMinReceita.text.toString().isNotEmpty() && tvMinReceita.text.toString() != "R$ 0,00") {
                        if (tvReceita.text.toString().replace("R$ ", "").replace(",", ".").replace("-", "").toFloat() >
                            tvMinReceita.text.toString().replace("R$ ", "").replace(",", ".").replace("-", "").toFloat()) {
                            tvReceita.setTextColor(Color.parseColor("#00A351"))
                        } else if (tvReceita.text.toString().replace("R$ ", "").replace(",", ".").replace("-", "").toFloat() <
                            tvMinReceita.text.toString().replace("R$ ", "").replace(",", ".").replace("-", "").toFloat()) {
                            tvReceita.setTextColor(Color.parseColor("#B84A43"))
                        }
                    }

                    progressBar?.dismiss()
                }
            }
            anyChartView.setChart(pie)
        }

        override fun onCancelled(databaseError: DatabaseError) {}
    }

    private fun getMetas() {
        valList = mutableListOf()

        val queryMetas = FirebaseDatabase.getInstance().getReference("meta")
            .orderByChild("usuario")
            .equalTo(firebaseUser?.getEmail().toString())

        queryMetas.addListenerForSingleValueEvent(metasEventListener)
    }

    var metasEventListener: ValueEventListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            valList.clear()

            for (snapshot in dataSnapshot.children) {
                val cat = snapshot.getValue(Meta::class.java)
                valList.add(cat!!)
            }

            for (i in valList.indices) {
                if (valList[i].mes.contains(optionSpinner)) {
                    tvEcon.setText(valList[i].econ)
                    tvMaxValor.setText(valList[i].max)
                    tvMinReceita.setText(valList[i].min)
                } else {
                    tvEcon.setText("R$ 0,00")
                    tvMaxValor.setText("R$ 0,00")
                    tvMinReceita.setText("R$ 0,00")

                    Log.d("sera" , optionSpinner)
                }
            }

            progressBar?.dismiss()
        }

        override fun onCancelled(databaseError: DatabaseError) {}
    }

    fun progressBar(): AlertDialog {
        val builder = AlertDialog.Builder(this)

        val inflater = LayoutInflater.from(this)

        val view = inflater.inflate(R.layout.progress_bar, null)

        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        progressBar.getIndeterminateDrawable().setColorFilter(
            Color.rgb(0,163,81), android.graphics.PorterDuff.Mode.SRC_IN)

        builder.setView(view)

        val alert = builder.create()

        alert.show()
        alert.setCancelable(false)
        alert.setCanceledOnTouchOutside(false)

        return alert
    }
}
