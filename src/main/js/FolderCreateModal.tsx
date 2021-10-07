import React, { FC, useState } from "react";
import { ButtonGroup, InputField, Modal, Textarea, Button } from "@scm-manager/ui-components";
import { useTranslation } from "react-i18next";
import { File } from "@scm-manager/ui-types";

type Result = {
  name: string;
  message: string;
};

type Props = {
  revision?: string;
  path?: string;
  sources: File;
  onClose: () => void;
  onSubmit: (result: Result) => void;
};

const FolderCreateModal: FC<Props> = ({ sources, revision, onClose, onSubmit }) => {
  const [t] = useTranslation("plugins");
  const [folderName, setFolderName] = useState("");
  const [commitMessage, setCommitMessage] = useState("");

  const body = (
    <>
      <InputField label={t("scm-manage-folder-plugin.create.path.label")} value={"/" + sources.path} disabled={true} />
      <InputField label={t("scm-manage-folder-plugin.create.name.label")} value={folderName} onChange={setFolderName} />
      <Textarea
        placeholder={t("scm-manage-folder-plugin.create.commit.placeholder")}
        onChange={message => setCommitMessage(message)}
        value={commitMessage}
      />
      <InputField label={t("scm-manage-folder-plugin.create.branch.label")} value={revision} disabled={true} />
      {/*<CommitAuthor />*/}
    </>
  );

  const footer = (
    <ButtonGroup>
      <Button className="is-marginless" action={onClose}>
        {t("scm-manage-folder-plugin.create.cancel.label")}
      </Button>
      <Button
        className="is-marginless"
        action={() => onSubmit({ message: commitMessage, name: folderName })}
        color="primary"
      >
        {t("scm-manage-folder-plugin.create.submit.label")}
      </Button>
    </ButtonGroup>
  );

  return (
    <Modal
      body={body}
      footer={footer}
      title={t("scm-manage-folder-plugin.create.title")}
      closeFunction={onClose}
      active={true}
    />
  );
};

export default FolderCreateModal;
