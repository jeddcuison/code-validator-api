package org.sitenv.vocabularies.validation.validators;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.sitenv.vocabularies.configuration.ConfiguredValidationResultSeverityLevel;
import org.sitenv.vocabularies.configuration.ConfiguredValidator;
import org.sitenv.vocabularies.validation.VocabularyNodeValidator;
import org.sitenv.vocabularies.validation.dto.NodeValidationResult;
import org.sitenv.vocabularies.validation.dto.VocabularyValidationResult;
import org.sitenv.vocabularies.validation.dto.enums.VocabularyValidationResultLevel;
import org.sitenv.vocabularies.validation.repositories.VsacValuesSetRepository;
import org.sitenv.vocabularies.validation.utils.XpathUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component(value = "CcdaLanguageCodeNodeLanguageCodeValuesetValidator")
public class CcdaLanguageCodeNodeLanguageCodeValuesetValidator extends BaseValidator implements VocabularyNodeValidator {
	private static final Logger logger = Logger.getLogger(CcdaLanguageCodeNodeLanguageCodeValuesetValidator.class);
	private VsacValuesSetRepository vsacValuesSetRepository;

	@Autowired
	public CcdaLanguageCodeNodeLanguageCodeValuesetValidator(VsacValuesSetRepository vsacValuesSetRepository) {
		this.vsacValuesSetRepository = vsacValuesSetRepository;
	}

	@Override
	public List<VocabularyValidationResult> validateNode(ConfiguredValidator configuredValidator, XPath xpath, Node node, int nodeIndex) {
		List<String> allowedConfiguredCodeSystemOids = new ArrayList<>(Arrays.asList(configuredValidator.getAllowedValuesetOids().split(",")));

		initializeValuesFromNodeAttributesToBeValidated(xpath, node);

		NodeValidationResult nodeValidationResult = new NodeValidationResult();
		nodeValidationResult.setValidatedDocumentXpathExpression(XpathUtils.buildXpathFromNode(node));
		nodeValidationResult.setRequestedCode(nodeCode);
		nodeValidationResult.setConfiguredAllowableValuesetOidsForNode(configuredValidator.getAllowedValuesetOids());

		if(vsacValuesSetRepository.valuesetOidsExists(allowedConfiguredCodeSystemOids)){
			nodeValidationResult.setNodeValuesetsFound(true);
			if(StringUtils.contains(nodeCode, "-")){
				nodeCode = StringUtils.substringBefore(nodeCode, "-");
			}
			if (vsacValuesSetRepository.codeExistsInValueset(nodeCode, allowedConfiguredCodeSystemOids)) {
				nodeValidationResult.setValid(true);
			}
		}
		return buildVocabularyValidationResults(nodeValidationResult, configuredValidator.getConfiguredValidationResultSeverityLevel());
	}

	@Override
	protected List<VocabularyValidationResult> buildVocabularyValidationResults(NodeValidationResult nodeValidationResult, ConfiguredValidationResultSeverityLevel configuredNodeAttributeSeverityLevel) {
		List<VocabularyValidationResult> vocabularyValidationResults = new ArrayList<>();
		if(!nodeValidationResult.isValid()) {
			if (nodeValidationResult.isNodeValuesetsFound()) {
				VocabularyValidationResult vocabularyValidationResult = new VocabularyValidationResult();
				vocabularyValidationResult.setNodeValidationResult(nodeValidationResult);
				vocabularyValidationResult.setVocabularyValidationResultLevel(VocabularyValidationResultLevel.valueOf(configuredNodeAttributeSeverityLevel.getCodeSeverityLevel()));
                String validationMessage = "Code '" + nodeCode + " (from " + nodeValidationResult.getRequestedCode()+ ")' does not exist in the value set (" + nodeValidationResult.getConfiguredAllowableValuesetOidsForNode() + ")";
				vocabularyValidationResult.setMessage(validationMessage);
				vocabularyValidationResults.add(vocabularyValidationResult);
			}else{
				vocabularyValidationResults.add(valuesetNotLoadedResult(nodeValidationResult));
			}
		}
		return vocabularyValidationResults;
	}

}
