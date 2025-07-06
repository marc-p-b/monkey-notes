package net.kprod.mn.data;

import net.kprod.mn.data.enums.ViewOptionsCompletionStatus;

public class ViewOptions {

        public static ViewOptions all() {
            return new ViewOptions()
                    .setCompletionStatus(ViewOptionsCompletionStatus.all);
        }

        private ViewOptionsCompletionStatus completionStatus;

        public ViewOptions setCompletionStatus(ViewOptionsCompletionStatus completionStatus) {
            this.completionStatus = completionStatus;
            return this;
        }

        public ViewOptionsCompletionStatus getCompletionStatus() {
            return completionStatus;
        }
    }