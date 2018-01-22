package com.r3.workflows.generation.stages

import co.paralleluniverse.fibers.Suspendable
import com.r3.workflows.generation.ContinuingTransition
import com.squareup.kotlinpoet.*
import info.leadinglight.jdot.Node
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType

interface Stage<T : LinearState> {

    fun property(): KProperty1<T, Any?>

    fun toFlow(workflowName: String, transition: ContinuingTransition<*, *>): TypeSpec

    fun toDot(workflowName: String, transition: ContinuingTransition<*, *>): Node

    companion object {
        fun getContractNameForWorkflow(name: String): String {
            return getWorkflowName(name) + "Contract"
        }

        fun getWorkflowName(name: String) = name + "Workflow"
    }

    fun buildInitiatorType(classOfInput: KClass<*>): TypeSpec.Builder {
        val initiatorBuilder = TypeSpec.classBuilder("Inititator")
                .addAnnotation(InitiatingFlow::class)
                .addAnnotation(StartableByRPC::class)
                .primaryConstructor(FunSpec.constructorBuilder().addParameter("val input", classOfInput).build())
                .superclass(ParameterizedTypeName.get(FlowLogic::class, UniqueIdentifier::class))
        return initiatorBuilder
    }

    fun <T : LinearState> buildLookup(classOfState: KClass<T>): FunSpec.Builder {
        return FunSpec.builder("loadInput")
                .addParameter("identifier", UniqueIdentifier::class)
                .returns(ParameterizedTypeName.get(StateAndRef::class, classOfState))
                .addModifiers(KModifier.PRIVATE, KModifier.INLINE)
                .addStatement("val criteria = %T(linearId = listOf(identifier), status=%T.UNCONSUMED)",
                        QueryCriteria.LinearStateQueryCriteria::class,
                        Vault.StateStatus::class)
                .addStatement("return serviceHub.vaultService.queryBy<%T>(%T::class.java, criteria).states.single()", classOfState, classOfState)
    }

    fun buildTypeNameFromType(type: Any): TypeName {

        return when (type) {
            is KClass<*> -> {
                ClassName.bestGuess(type.qualifiedName!!)
            }
            is KType -> {
                val varArgs = type.arguments.map { it.type!!.classifier as KClass<*> }.map { ClassName.bestGuess(it.qualifiedName!!) }.toTypedArray()
                val className = ClassName.bestGuess((type.classifier as KClass<*>).qualifiedName!!)
                if (!varArgs.isEmpty()) ParameterizedTypeName.get(className, *varArgs) else className
            }
            else -> {
                throw IllegalStateException();
            }
        }

    }

    fun buildInitiatorForUpdateType(vararg namesAndTypes: Pair<String, Any>): TypeSpec.Builder {
        val constructorBuilder = FunSpec.constructorBuilder()

        namesAndTypes.forEach { nameAndType ->
            val name: String = nameAndType.first;
            val type = nameAndType.second;


            constructorBuilder.addParameter("val " + name, buildTypeNameFromType(type))
        }

        val initiatorBuilder = TypeSpec.classBuilder("Inititator")
                .addAnnotation(InitiatingFlow::class)
                .addAnnotation(StartableByRPC::class)
                .primaryConstructor(constructorBuilder
                        .build())
                .superclass(ParameterizedTypeName.get(FlowLogic::class, UniqueIdentifier::class))
        return initiatorBuilder
    }

    fun buildDumbResponderFlow(flowObject: TypeSpec.Builder) {

        val responderBuilder = TypeSpec.classBuilder("Responder")
                .addAnnotation(InitiatingFlow::class)
                .addAnnotation(AnnotationSpec.builder(InitiatedBy::class).addMember("Inititator::class").build())
                .primaryConstructor(FunSpec.constructorBuilder().addParameter("val counterpartySession", FlowSession::class).build())
                .superclass(ParameterizedTypeName.get(FlowLogic::class, SignedTransaction::class))

        val callBuilder = FunSpec.builder("call").returns(SignedTransaction::class).addModifiers(KModifier.OVERRIDE).addAnnotation(Suspendable::class)


        val blindSignText = """
                val flow = object : %T(counterpartySession) {
                    @Suspendable
                    override fun checkTransaction(stx: SignedTransaction) {
                        //all checks delegated to contract
                    }
                }

                val stx = subFlow(flow)
                return waitForLedgerCommit(stx.id)
            """


        callBuilder.addStatement(blindSignText, SignTransactionFlow::class)
        responderBuilder.addFunction(callBuilder.build())
        flowObject.addType(responderBuilder.build())


    }

}